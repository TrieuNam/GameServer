package com.SouthMillion.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Controller để quản lý các Java processes đang chạy.
 *
 * Dùng PowerShell -EncodedCommand (Base64 UTF-16LE) thay vì -Command để tránh
 * lỗi quoting/escaping khi truyền script phức tạp từ Java sang PowerShell.
 * Dùng netstat -ano cho port detection (không cần admin, reliable trên mọi Windows).
 */
@Slf4j
@RestController
@RequestMapping("/api/processes")
@CrossOrigin(origins = "*")
public class ProcessController {

    private static final int PORT_MIN = 7000;
    private static final int PORT_MAX = 9999;
    // Delimiter an toàn thay thế | trong CommandLine (để tránh split conflict)
    private static final String PIPE_PLACEHOLDER = "%%PIPE%%";

    // ─────────────────────────────────────────────────────────────────────────
    // API Endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listJavaProcesses() {
        List<Map<String, Object>> processes = new ArrayList<>();

        try {
            // Bước 1: Get PID + memory từ Get-Process (đơn giản, không cần WMI)
            Map<String, String> memMap = getProcessMemory();

            // Bước 2: Get PID + CommandLine từ WMI (qua EncodedCommand)
            Map<String, String> cmdMap = getProcessCommandLines();

            // Bước 3: Get PID → port map từ netstat
            Map<String, String> portMap = buildPidPortMap();

            for (Map.Entry<String, String> entry : memMap.entrySet()) {
                String pid = entry.getKey();
                String memMB = entry.getValue();

                String cmdLine = cmdMap.getOrDefault(pid, "");
                String serviceName = detectServiceName(cmdLine);
                String port = portMap.getOrDefault(pid, "N/A");

                Map<String, Object> info = new HashMap<>();
                info.put("pid", pid);
                info.put("serviceName", serviceName);
                info.put("processName", "java");
                info.put("startTime", "Running");
                info.put("memory", memMB);
                info.put("port", port);
                processes.add(info);

                log.info("Java process: PID={} Service={} Memory={}MB Port={}",
                    pid, serviceName, memMB, port);
            }
        } catch (Exception e) {
            log.error("Error listing processes", e);
        }

        return ResponseEntity.ok(processes);
    }

    @GetMapping("/ports")
    public ResponseEntity<List<Map<String, Object>>> listActivePorts() {
        List<Map<String, Object>> ports = new ArrayList<>();

        try {
            Set<String> javaPids = getJavaPids();
            if (javaPids.isEmpty()) return ResponseEntity.ok(ports);

            Map<String, String> portMap = buildPidPortMap();
            for (Map.Entry<String, String> e : portMap.entrySet()) {
                String pid = e.getKey();
                String port = e.getValue();
                if (javaPids.contains(pid)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("port", port);
                    item.put("pid", pid);
                    item.put("processName", "java");
                    ports.add(item);
                }
            }
        } catch (Exception e) {
            log.error("Error listing ports", e);
        }

        return ResponseEntity.ok(ports);
    }

    @PostMapping("/kill/{pid}")
    public ResponseEntity<Map<String, Object>> killProcess(@PathVariable Long pid) {
        Map<String, Object> result = new HashMap<>();
        try {
            String script = "Stop-Process -Id " + pid + " -Force -ErrorAction Stop";
            runPsEncoded(script);
            result.put("success", true);
            result.put("message", "Process " + pid + " stopped");
            log.info("Killed process: {}", pid);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/kill-all")
    public ResponseEntity<Map<String, Object>> killAllJavaProcesses() {
        Map<String, Object> result = new HashMap<>();
        try {
            long currentPid = ProcessHandle.current().pid();
            String script =
                "Get-Process -Name java,javaw -ErrorAction SilentlyContinue | " +
                "Where-Object { $_.Id -ne " + currentPid + " } | " +
                "Stop-Process -Force";
            runPsEncoded(script);
            result.put("success", true);
            result.put("message", "All Java processes stopped (except admin-service PID " + currentPid + ")");
            log.info("Killed all Java processes except PID: {}", currentPid);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Chạy PowerShell script an toàn bằng -EncodedCommand (Base64 UTF-16LE).
     * Tránh hoàn toàn vấn đề escape/quoting khi truyền script từ Java.
     */
    private List<String> runPsEncoded(String script) {
        try {
            byte[] utf16le = script.getBytes(StandardCharsets.UTF_16LE);
            String encoded = Base64.getEncoder().encodeToString(utf16le);

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-EncodedCommand", encoded
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String t = line.trim();
                    if (!t.isEmpty()) lines.add(t);
                }
            }
            process.waitFor(30, TimeUnit.SECONDS);
            return lines;
        } catch (Exception e) {
            log.debug("PowerShell error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lấy PID → MemoryMB của tất cả java/javaw processes.
     * Dùng Get-Process (không cần WMI, không cần admin).
     */
    private Map<String, String> getProcessMemory() {
        Map<String, String> map = new LinkedHashMap<>();
        String script =
            "$procs = @(Get-Process -Name java,javaw -ErrorAction SilentlyContinue)\n" +
            "foreach ($p in $procs) {\n" +
            "    $mem = [math]::Round($p.WorkingSet64 / 1MB, 1)\n" +
            "    Write-Output \"$($p.Id)|$mem\"\n" +
            "}";

        for (String line : runPsEncoded(script)) {
            String[] parts = line.split("\\|", 2);
            if (parts.length == 2 && parts[0].matches("\\d+")) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }

    /**
     * Lấy PID → CommandLine của tất cả java/javaw processes qua WMI.
     * Dùng Get-CimInstance (thay thế mới của Get-WmiObject, PowerShell 3+).
     * Thay thế ký tự | trong CommandLine bằng placeholder để tránh split conflict.
     */
    private Map<String, String> getProcessCommandLines() {
        Map<String, String> map = new HashMap<>();
        String script =
            "$procs = @(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" " +
            "-ErrorAction SilentlyContinue)\n" +
            "foreach ($p in $procs) {\n" +
            "    $cmd = if ($p.CommandLine) { $p.CommandLine -replace '\\|', '" + PIPE_PLACEHOLDER + "' } else { '' }\n" +
            "    Write-Output \"$($p.ProcessId)|$cmd\"\n" +
            "}";

        for (String line : runPsEncoded(script)) {
            String[] parts = line.split("\\|", 2);
            if (parts.length == 2 && parts[0].matches("\\d+")) {
                // Khôi phục ký tự | trong CommandLine
                String cmdLine = parts[1].trim().replace(PIPE_PLACEHOLDER, "|");
                map.put(parts[0].trim(), cmdLine);
            }
        }
        return map;
    }

    /**
     * Lấy set tất cả PIDs của java/javaw.
     */
    private Set<String> getJavaPids() {
        Set<String> pids = new HashSet<>();
        String script =
            "@(Get-Process -Name java,javaw -ErrorAction SilentlyContinue) | " +
            "ForEach-Object { Write-Output $_.Id }";
        for (String line : runPsEncoded(script)) {
            if (line.matches("\\d+")) pids.add(line);
        }
        return pids;
    }

    /**
     * Build map: PID → port (LISTENING, trong range PORT_MIN–PORT_MAX).
     * Dùng netstat -ano thay Get-NetTCPConnection vì:
     *  - Get-NetTCPConnection yêu cầu admin để lấy OwningProcess
     *  - netstat -ano hoạt động không cần admin, mọi Windows
     */
    private Map<String, String> buildPidPortMap() {
        Map<String, String> map = new HashMap<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-ano");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // "TCP  0.0.0.0:8761  0.0.0.0:0  LISTENING  12345"
                    if (!line.startsWith("TCP") || !line.contains("LISTENING")) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length < 5) continue;

                    String localAddr = parts[1];
                    String pid = parts[4];

                    int colonIdx = localAddr.lastIndexOf(':');
                    if (colonIdx < 0) continue;

                    try {
                        int port = Integer.parseInt(localAddr.substring(colonIdx + 1));
                        if (port >= PORT_MIN && port <= PORT_MAX) {
                            map.putIfAbsent(pid, String.valueOf(port));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("netstat error: {}", e.getMessage());
        }
        return map;
    }

    /**
     * Detect tên service từ CommandLine.
     * VD: "...role-service-0.0.1.jar..." → "role-service"
     */
    private String detectServiceName(String cmdLine) {
        if (cmdLine == null || cmdLine.isEmpty()) return "Java Process";

        String lower = cmdLine.toLowerCase();

        // Tìm tên JAR: "role-service-0.0.1.jar" → "role-service"
        int jarIdx = lower.lastIndexOf(".jar");
        if (jarIdx > 0) {
            int start = Math.max(0,
                Math.max(lower.lastIndexOf("/", jarIdx),
                         lower.lastIndexOf("\\", jarIdx)) + 1);
            String jarName = cmdLine.substring(start, jarIdx);
            jarName = jarName.replaceAll("-\\d+\\.\\d+.*$", "");
            if (!jarName.isEmpty()) return jarName;
        }

        // Fallback: tìm spring.application.name=
        int nameIdx = lower.indexOf("spring.application.name=");
        if (nameIdx >= 0) {
            int end = cmdLine.indexOf(' ', nameIdx);
            String arg = end > 0 ? cmdLine.substring(nameIdx, end) : cmdLine.substring(nameIdx);
            String[] kv = arg.split("=", 2);
            if (kv.length == 2 && !kv[1].isEmpty()) return kv[1];
        }

        return "Java Process";
    }
}
