package com.SouthMillion.admin.service;

import com.SouthMillion.admin.entity.ServiceConfig;
import com.SouthMillion.admin.entity.ServiceStatus;
import com.SouthMillion.admin.repository.ServiceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service Process Manager
 * Manages lifecycle of GameServer microservices (start/stop/restart)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceManager {

    private final ServiceConfigRepository configRepository;
    private final DockerManager dockerManager;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gameserver.base.path:D:/project/serverGame/GameServer}")
    private String baseGameServerPath;

    @Value("${gameserver.cds.enabled:false}")
    private boolean cdsEnabled;

    /**
     * Map to track running processes: serviceName -> Process
     */
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    /**
     * Map to track log outputs: serviceName -> List<String>
     */
    private final Map<String, List<String>> serviceLogs = new ConcurrentHashMap<>();

    /**
     * Start a service
     * NOTE: @Transactional intentionally removed — this is a long-running async operation
     * (can take 60+ seconds waiting for health check). Holding a DB connection open that long
     * exhausts HikariCP pool when multiple services start concurrently (P1 = 13 services).
     * Each repository.save() call creates its own short transaction via the repository layer.
     */
    @Async
    public CompletableFuture<Boolean> startService(String serviceName) {
        log.info("🚀 Starting service: {}", serviceName);

        Optional<ServiceConfig> configOpt = configRepository.findByServiceName(serviceName);
        if (configOpt.isEmpty()) {
            log.error("❌ Service not found: {}", serviceName);
            return CompletableFuture.completedFuture(false);
        }

        ServiceConfig config = configOpt.get();
        if (!config.getEnabled()) {
            log.warn("⚠️ Service is disabled: {}", serviceName);
            return CompletableFuture.completedFuture(false);
        }

        // Check if already running
        if (runningProcesses.containsKey(serviceName) && isProcessAlive(serviceName)) {
            log.warn("⚠️ Service already running: {}", serviceName);
            return CompletableFuture.completedFuture(true);
        }

        try {
            // Update status to STARTING
            config.setStatus(ServiceStatus.STARTING);
            config.setLastStarted(LocalDateTime.now());
            configRepository.save(config);

            // SPECIAL CASE: Eureka starts core infrastructure (Redis, Kafka) if not already running
            // NOTE: Eureka itself doesn't need Redis/Kafka, but other P0 services do
            if ("eureka-server".equals(serviceName)) {
                log.info("🚀 Eureka detected - Starting P0 infrastructure (Redis, Kafka)...");
                boolean infraStarted = dockerManager.startAllInfrastructure();
                if (!infraStarted) {
                    log.warn("⚠️ Failed to start infrastructure for Eureka - continuing anyway (Eureka doesn't require them)");
                    // Don't fail - Eureka can run without Redis/Kafka
                }
                log.info("✅ P0 infrastructure check completed!");
            }
            // NORMAL CASE: Start Docker container if required (for individual service start)
            // Note: When starting via phase, Docker containers are started at phase level
            else if (config.getRequiresDocker() && config.getContainerName() != null) {
                log.info("🐳 Checking Docker container: {}", config.getContainerName());
                
                // Check if container is already running
                if (!dockerManager.isContainerRunning(config.getContainerName())) {
                    log.info("🐳 Starting Docker container: {}", config.getContainerName());
                    boolean dockerStarted = dockerManager.startContainer(config.getContainerName());
                    if (!dockerStarted) {
                        log.error("❌ Failed to start Docker container for: {}", serviceName);
                        config.setStatus(ServiceStatus.ERROR);
                        configRepository.save(config);
                        return CompletableFuture.completedFuture(false);
                    }
                    log.info("✅ Docker container started: {}", config.getContainerName());
                } else {
                    log.info("✅ Docker container already running: {}", config.getContainerName());
                }
            }

            // Build command
            List<String> command = buildStartCommand(config);
            log.info("🚀 Command: {}", String.join(" ", command));
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Set working directory - resolve relative paths to absolute
            String workingDir = config.getWorkingDirectory();
            if (workingDir != null) {
                // If relative path, resolve from base path
                File workDir = new File(workingDir);
                if (!workDir.isAbsolute()) {
                    workDir = new File(baseGameServerPath, workingDir.replace("..", "."));
                }
                processBuilder.directory(workDir);
                log.info("📁 Working directory: {}", workDir.getAbsolutePath());
            } else {
                // Fallback to service folder
                File workDir = new File(baseGameServerPath, serviceName);
                processBuilder.directory(workDir);
                log.info("📁 Working directory (default): {}", workDir.getAbsolutePath());
            }

            // Redirect error stream to output
            processBuilder.redirectErrorStream(true);

            // Start process
            log.info("🔥 Starting process...");
            Process process = processBuilder.start();
            
            // Check if process started successfully
            if (!process.isAlive()) {
                log.error("❌ Process failed to start immediately!");
                config.setStatus(ServiceStatus.ERROR);
                configRepository.save(config);
                return CompletableFuture.completedFuture(false);
            }
            
            runningProcesses.put(serviceName, process);

            // Get PID
            long pid = process.pid();
            config.setProcessId(pid);
            log.info("✅ Process started with PID: {}", pid);

            // Start log reader thread
            startLogReader(serviceName, process);
            
            // Give it a moment to start logging
            Thread.sleep(2000);

            // Wait for service to be healthy
            boolean healthy = waitForHealthCheck(config, 60);

            if (healthy) {
                config.setStatus(ServiceStatus.RUNNING);
                log.info("✅ Service started successfully: {} (Port: {})", serviceName, config.getPort());
            } else {
                config.setStatus(ServiceStatus.ERROR);
                log.error("❌ Service failed health check: {}", serviceName);
            }

            configRepository.save(config);
            return CompletableFuture.completedFuture(healthy);

        } catch (Exception e) {
            log.error("❌ Failed to start service: {}", serviceName, e);
            config.setStatus(ServiceStatus.ERROR);
            configRepository.save(config);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Stop a service
     */
    @Transactional
    public boolean stopService(String serviceName) {
        log.info("🛑 Stopping service: {}", serviceName);

        Optional<ServiceConfig> configOpt = configRepository.findByServiceName(serviceName);
        if (configOpt.isEmpty()) {
            log.error("❌ Service not found: {}", serviceName);
            return false;
        }

        ServiceConfig config = configOpt.get();
        Process process = runningProcesses.get(serviceName);

        if (process == null || !process.isAlive()) {
            log.warn("⚠️ Service is not running: {}", serviceName);
            config.setStatus(ServiceStatus.STOPPED);
            configRepository.save(config);
            return true;
        }

        try {
            // Update status
            config.setStatus(ServiceStatus.STOPPING);
            config.setLastStopped(LocalDateTime.now());
            configRepository.save(config);

            // Try graceful shutdown first (via actuator)
            try {
                String shutdownUrl = "http://localhost:" + config.getPort() + "/actuator/shutdown";
                restTemplate.postForObject(shutdownUrl, null, String.class);
                log.info("✅ Sent graceful shutdown signal to: {}", serviceName);
            } catch (Exception e) {
                log.warn("⚠️ Graceful shutdown failed, using force kill");
            }

            // Wait for graceful shutdown
            boolean exited = process.waitFor(30, TimeUnit.SECONDS);

            if (!exited) {
                // Force kill if still alive
                log.warn("⚠️ Force killing service: {}", serviceName);
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }

            // Cleanup
            runningProcesses.remove(serviceName);
            serviceLogs.remove(serviceName);

            config.setStatus(ServiceStatus.STOPPED);
            config.setProcessId(null);
            configRepository.save(config);

            log.info("✅ Service stopped: {}", serviceName);

            // Best-effort cleanup: once the JVM is down, delete the service's CDS archive so
            // Windows `mvn clean` won't fail on a stale locked `*-cds.jsa` file.
            deleteCdsArchive(serviceName);

            // Stop Docker container if required
            if (config.getRequiresDocker() && config.getContainerName() != null) {
                log.info("🐳 Stopping Docker container: {}", config.getContainerName());
                boolean dockerStopped = dockerManager.stopContainer(config.getContainerName());
                if (!dockerStopped) {
                    log.warn("⚠️ Failed to stop Docker container: {}", config.getContainerName());
                } else {
                    log.info("✅ Docker container stopped: {}", config.getContainerName());
                }
            }

            return true;

        } catch (Exception e) {
            log.error("❌ Failed to stop service: {}", serviceName, e);
            return false;
        }
    }

    /**
     * Restart a service
     */
    public CompletableFuture<Boolean> restartService(String serviceName) {
        log.info("🔄 Restarting service: {}", serviceName);
        stopService(serviceName);
        try {
            Thread.sleep(2000); // Wait 2 seconds between stop and start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return startService(serviceName);
    }

    /**
     * Get service status
     */
    public ServiceStatus getServiceStatus(String serviceName) {
        Optional<ServiceConfig> config = configRepository.findByServiceName(serviceName);
        if (config.isEmpty()) {
            return ServiceStatus.UNKNOWN;
        }

        // Check if process is actually running
        if (runningProcesses.containsKey(serviceName) && isProcessAlive(serviceName)) {
            return ServiceStatus.RUNNING;
        } else if (config.get().getStatus() == ServiceStatus.RUNNING) {
            // Update status if process died
            config.get().setStatus(ServiceStatus.STOPPED);
            configRepository.save(config.get());
            return ServiceStatus.STOPPED;
        }

        return config.get().getStatus();
    }

    /**
     * Get service logs
     */
    public List<String> getServiceLogs(String serviceName, int lines) {
        List<String> logs = serviceLogs.getOrDefault(serviceName, new ArrayList<>());
        int size = logs.size();
        if (lines <= 0 || lines >= size) {
            return new ArrayList<>(logs);
        }
        return new ArrayList<>(logs.subList(size - lines, size));
    }

    /**
     * Start all services by priority order
     */
    @Async
    public CompletableFuture<Map<String, Boolean>> startAllServices() {
        log.info("🚀 Starting all services in priority order...");

        List<ServiceConfig> services = configRepository.findAllByStartupOrder();
        Map<String, Boolean> results = new LinkedHashMap<>();

        for (ServiceConfig service : services) {
            if (!service.getEnabled()) {
                continue;
            }

            log.info("📦 Starting: {} (Order: {})", service.getServiceName(), service.getStartupOrder());
            CompletableFuture<Boolean> result = startService(service.getServiceName());

            try {
                Boolean success = result.get(120, TimeUnit.SECONDS); // 2 minutes timeout per service
                results.put(service.getServiceName(), success);

                if (success) {
                    log.info("✅ Started: {}", service.getServiceName());
                } else {
                    log.error("❌ Failed: {}", service.getServiceName());
                }

                // Wait between services
                Thread.sleep(3000);

            } catch (Exception e) {
                log.error("❌ Error starting: {}", service.getServiceName(), e);
                results.put(service.getServiceName(), false);
            }
        }

        log.info("✅ All services startup complete!");
        return CompletableFuture.completedFuture(results);
    }

    /**
     * Stop all services (reverse order)
     */
    public Map<String, Boolean> stopAllServices() {
        log.info("🛑 Stopping all services...");

        List<ServiceConfig> services = configRepository.findAllByStartupOrder();
        Collections.reverse(services); // Stop in reverse order
        Map<String, Boolean> results = new LinkedHashMap<>();

        for (ServiceConfig service : services) {
            log.info("🛑 Stopping: {}", service.getServiceName());
            boolean success = stopService(service.getServiceName());
            results.put(service.getServiceName(), success);
        }

        log.info("✅ All services stopped!");
        return results;
    }

    /**
     * Build start command for service
     */
    private List<String> buildStartCommand(ServiceConfig config) {
        List<String> command = new ArrayList<>();
        command.add("java");

        // Add JVM args
        if (config.getJvmArgs() != null && !config.getJvmArgs().isEmpty()) {
            command.addAll(Arrays.asList(config.getJvmArgs().split("\\s+")));
        } else {
            // Fallback tier detection — use serviceName (reliable) not description (fragile)
            String svcName = config.getServiceName();

            if (svcName.contains("eureka") || svcName.contains("gateway") || svcName.contains("config") || svcName.contains("webSocket")) {
                // CRITICAL TIER: Infrastructure services
                command.add("-Xms128m");
                command.add("-Xmx256m");
                command.add("-Xss2m");
                log.info("🔥 CRITICAL profile: -Xms128m -Xmx256m -Xss2m");
            } else if (svcName.contains("analytics") || svcName.contains("scheduler") ||
                       svcName.contains("file") || svcName.contains("localization") || svcName.contains("moderation")) {
                // BACKGROUND TIER: Low-traffic support services
                command.add("-Xms48m");
                command.add("-Xmx96m");
                command.add("-Xss2m");
                log.info("⚡ BACKGROUND profile: -Xms48m -Xmx96m -Xss2m");
            } else {
                // STANDARD TIER: Game feature services
                command.add("-Xms64m");
                command.add("-Xmx128m");
                command.add("-Xss2m");
                log.info("🎯 STANDARD profile: -Xms64m -Xmx128m -Xss2m");
            }
        }

        // Raise known-too-small service profiles before appending shared optimization flags.
        ensureServiceJvmFloor(config.getServiceName(), command);

        // Always append memory-saving flags that are missing from DB jvm_args.
        // These flags are NOT set in any service's jvm_args column, so appending is safe.
        // Last-wins rule: if a flag appears twice, the last value wins — no crash.
        appendMemoryOptimizationFlags(command);

        // CDS (Class Data Sharing): optional and disabled by default in dev.
        // Enable with `gameserver.cds.enabled=true` when you want startup optimization.
        if (cdsEnabled) {
            String cdsArchivePath = resolveCdsArchivePath(config.getServiceName());
            File cdsArchive = new File(cdsArchivePath);
            if (cdsArchive.exists()) {
                command.add("-XX:SharedArchiveFile=" + cdsArchivePath);
                log.info("📦 CDS archive attached: {}", cdsArchivePath);
            }
        }

        // Add JAR - resolve relative paths and auto-detect actual jar file
        String jarPath = config.getJarPath();
        File jarFile = null;
        
        if (jarPath == null || jarPath.isEmpty()) {
            // Auto-detect jar file in target directory
            File targetDir = new File(baseGameServerPath, config.getServiceName() + "/target");
            log.debug("Looking for JAR in: {}", targetDir.getAbsolutePath());
            
            if (targetDir.exists() && targetDir.isDirectory()) {
                File[] jarFiles = targetDir.listFiles((dir, name) -> 
                    name.startsWith(config.getServiceName()) && 
                    name.endsWith(".jar") && 
                    !name.endsWith("-original.jar")
                );
                
                if (jarFiles != null && jarFiles.length > 0) {
                    // Use the first matching jar file
                    jarFile = jarFiles[0];
                    log.info("🔍 Auto-detected JAR: {} (exists: {})", jarFile.getAbsolutePath(), jarFile.exists());
                } else {
                    // Fallback to default pattern - try common patterns
                    log.debug("No JAR found by filter, trying default patterns...");
                    String[] patterns = {
                        config.getServiceName() + "-1.0.0.jar",
                        config.getServiceName() + "-0.0.1-SNAPSHOT.jar"
                    };
                    
                    for (String pattern : patterns) {
                        File candidate = new File(targetDir, pattern);
                        if (candidate.exists()) {
                            jarFile = candidate;
                            log.info("🔍 Found JAR by pattern: {}", jarFile.getName());
                            break;
                        }
                    }
                    
                    if (jarFile == null) {
                        // Use first pattern as fallback (will trigger build if not exists)
                        jarFile = new File(targetDir, patterns[0]);
                        log.warn("⚠️ No JAR found, will use: {}", jarFile.getName());
                    }
                }
            } else {
                // Directory doesn't exist, use default
                log.warn("⚠️ Target directory does not exist: {}", targetDir.getAbsolutePath());
                jarFile = new File(baseGameServerPath, config.getServiceName() + "/target/" + config.getServiceName() + "-1.0.0.jar");
            }
        } else {
            // Use configured jar path
            jarFile = new File(jarPath);
            if (!jarFile.isAbsolute()) {
                // If starts with ../ convert to relative from base path
                if (jarPath.startsWith("../")) {
                    jarFile = new File(baseGameServerPath, jarPath.substring(3));
                } else {
                    jarFile = new File(baseGameServerPath, jarPath);
                }
            }
            log.info("📦 Using configured JAR path: {}", jarFile.getAbsolutePath());
        }
        
        // Verify jar file exists, if not, try to build it
        if (!jarFile.exists()) {
            log.warn("⚠️ JAR file not found: {}", jarFile.getAbsolutePath());
            log.info("🔨 Attempting to build service: {}", config.getServiceName());
            
            try {
                boolean buildSuccess = buildService(config.getServiceName());
                if (!buildSuccess) {
                    log.error("❌ Failed to build service: {}", config.getServiceName());
                    throw new RuntimeException("Failed to build service: " + config.getServiceName());
                }
                
                // After build, re-detect the JAR file that was actually created
                File targetDir = new File(baseGameServerPath, config.getServiceName() + "/target");
                if (targetDir.exists() && targetDir.isDirectory()) {
                    File[] jarFiles = targetDir.listFiles((dir, name) -> 
                        name.startsWith(config.getServiceName()) && 
                        name.endsWith(".jar") && 
                        !name.endsWith("-original.jar")
                    );
                    
                    if (jarFiles != null && jarFiles.length > 0) {
                        jarFile = jarFiles[0];
                        log.info("✅ Service built successfully, JAR found: {}", jarFile.getName());
                    } else {
                        log.error("❌ Build succeeded but no JAR found in: {}", targetDir.getAbsolutePath());
                        throw new RuntimeException("JAR file not found after build: " + config.getServiceName());
                    }
                } else {
                    log.error("❌ Target directory not found: {}", targetDir.getAbsolutePath());
                    throw new RuntimeException("Target directory not found: " + targetDir.getAbsolutePath());
                }
            } catch (Exception e) {
                log.error("❌ Error building service: {}", e.getMessage());
                throw new RuntimeException("Failed to build service: " + config.getServiceName(), e);
            }
        } else {
            log.info("✅ JAR file exists: {}", jarFile.getAbsolutePath());
        }

        // Prepare CDS archive on first start only when the feature is enabled.
        // On subsequent starts, the archive is already attached above via -XX:SharedArchiveFile.
        if (cdsEnabled) {
            prepareCdsArchive(config, jarFile);
        }

        command.add("-jar");
        command.add(jarFile.getAbsolutePath());
        log.info("📦 JAR: {}", jarFile.getAbsolutePath());

        // Add app args
        if (config.getAppArgs() != null && !config.getAppArgs().isEmpty()) {
            command.addAll(Arrays.asList(config.getAppArgs().split("\\s+")));
        }

        return command;
    }

    /**
     * Wait for service health check
     */
    private boolean waitForHealthCheck(ServiceConfig config, int timeoutSeconds) {
        String healthUrl = config.getHealthCheckUrl();
        int attempts = 0;
        int maxAttempts = timeoutSeconds / 5;

        while (attempts < maxAttempts) {
            try {
                Thread.sleep(5000); // Wait 5 seconds
                String response = restTemplate.getForObject(healthUrl, String.class);
                if (response != null && response.contains("UP")) {
                    return true;
                }
            } catch (Exception e) {
                // Service not ready yet
            }
            attempts++;
        }

        return false;
    }

    /**
     * Check if process is alive
     */
    private boolean isProcessAlive(String serviceName) {
        Process process = runningProcesses.get(serviceName);
        return process != null && process.isAlive();
    }

    /**
     * Start log reader thread
     */
    private void startLogReader(String serviceName, Process process) {
        serviceLogs.putIfAbsent(serviceName, Collections.synchronizedList(new ArrayList<>()));

        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                List<String> logs = serviceLogs.get(serviceName);

                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                    // Keep only last 1000 lines
                    if (logs.size() > 1000) {
                        logs.remove(0);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading logs for: {}", serviceName, e);
            }
        });

        logThread.setDaemon(true);
        logThread.setName("LogReader-" + serviceName);
        logThread.start();
    }
    
    /**
     * Build service using Maven
     */
    private boolean buildService(String serviceName) {
        try {
            File serviceDir = new File(baseGameServerPath, serviceName);
            if (!serviceDir.exists()) {
                log.error("❌ Service directory not found: {}", serviceDir.getAbsolutePath());
                return false;
            }
            
            log.info("🔨 Building service in: {}", serviceDir.getAbsolutePath());
            
            // Build Maven command based on OS
            List<String> command = new ArrayList<>();
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Windows: use mvn.cmd
                command.add("cmd");
                command.add("/c");
                command.add("mvn");
            } else {
                // Unix-like: use mvn directly
                command.add("mvn");
            }
            
            command.add("clean");
            command.add("package");
            command.add("-DskipTests");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(serviceDir);
            processBuilder.redirectErrorStream(true);
            
            log.info("🔨 Running: {}", String.join(" ", command));
            Process process = processBuilder.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Maven: {}", line);
                    if (line.contains("BUILD SUCCESS")) {
                        log.info("✅ Maven build successful");
                    } else if (line.contains("BUILD FAILURE")) {
                        log.error("❌ Maven build failed");
                    }
                }
            }
            
            // Wait for process to complete (max 5 minutes)
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                log.error("❌ Maven build timeout after 5 minutes");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("✅ Maven build completed successfully for: {}", serviceName);
                return true;
            } else {
                log.error("❌ Maven build failed with exit code: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Error building service: {}", serviceName, e);
            return false;
        }
    }
    
    /**
     * Guardrail for services that are known to become unstable with the legacy low-memory defaults.
     * Bag/role services load a much larger Spring + mapper surface and routinely exhaust the
     * standard 64m/128m fallback profile plus a 128m metaspace cap.
     */
    private void ensureServiceJvmFloor(String serviceName, List<String> command) {
        if (serviceName == null) {
            return;
        }

        String normalizedService = serviceName.trim().toLowerCase(Locale.ROOT);
        if (!"role-service".equals(normalizedService) && !"bag-service".equals(normalizedService)) {
            return;
        }

        upsertJvmMemoryArg(command, "-Xms", 256);
        upsertJvmMemoryArg(command, "-Xmx", 512);
        upsertJvmMemoryArg(command, "-XX:MetaspaceSize=", 128);
        upsertJvmMemoryArg(command, "-XX:MaxMetaspaceSize=", 256);
    }

    private void upsertJvmMemoryArg(List<String> command, String prefix, int minimumMb) {
        for (int i = 0; i < command.size(); i++) {
            String arg = command.get(i);
            if (!arg.startsWith(prefix)) {
                continue;
            }

            Integer currentMb = parseJvmMemoryInMb(arg.substring(prefix.length()));
            if (currentMb == null || currentMb < minimumMb) {
                command.set(i, prefix + minimumMb + "m");
            }
            return;
        }

        command.add(prefix + minimumMb + "m");
    }

    private Integer parseJvmMemoryInMb(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String value = rawValue.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("g")) {
                return Integer.parseInt(value.substring(0, value.length() - 1)) * 1024;
            }
            if (value.endsWith("m")) {
                return Integer.parseInt(value.substring(0, value.length() - 1));
            }
            if (value.endsWith("k")) {
                return Math.max(1, Integer.parseInt(value.substring(0, value.length() - 1)) / 1024);
            }
            return Integer.parseInt(value) / (1024 * 1024);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Append only safe runtime flags.
     *
     * The previous aggressive low-memory caps (`ReservedCodeCacheSize=64m`,
     * `MaxMetaspaceSize=128m`, `CompressedClassSpaceSize=64m`) were reverted after
     * causing `OutOfMemoryError: Metaspace` on Spring-heavy services such as bag/role.
     */
    private void appendMemoryOptimizationFlags(List<String> command) {
        String joined = String.join(" ", command);

        // Keep only non-capping flags that are generally safe for dev/runtime starts.
        if (!joined.contains("spring.jmx.enabled")) {
            command.add("-Dspring.jmx.enabled=false");
        }

        // Use G1GC for services with Xmx >= 256m.
        // Use SerialGC for services with Xmx <= 128m to avoid unnecessary GC worker overhead.
        if (!joined.contains("UseG1GC") && !joined.contains("UseSerialGC") && !joined.contains("UseZGC")) {
            boolean isSmallHeap = joined.contains("Xmx64m") || joined.contains("Xmx96m") || joined.contains("Xmx128m");
            if (isSmallHeap) {
                command.add("-XX:+UseSerialGC");
            } else {
                command.add("-XX:+UseG1GC");
                command.add("-XX:MaxGCPauseMillis=200");
            }
        }
    }

    /**
     * Build CDS archive for a service if it doesn't exist yet.
     *
     * CDS (Class Data Sharing) — Java standard feature, works on any JDK 11+.
     * Spring Boot 3.3+ integrates with CDS via -Dspring.context.exit=onRefresh.
     * The training run starts the app, lets Spring build the ApplicationContext,
     * dumps class metadata to a .jsa archive, then exits.
     * On subsequent starts: -XX:SharedArchiveFile=<path> loads the archive,
     * skipping class loading and verification → ~30-50% faster startup, ~15-25% less RAM.
     *
     * Why safe for this stack (Spring Cloud Netflix / JPA / Redis):
     * CDS is a JVM-level optimization — it caches bytecode, not Spring beans.
     * It works regardless of whether the library supports GraalVM native image.
     */
    public void prepareCdsArchive(ServiceConfig config, File jarFile) {
        if (!cdsEnabled) {
            log.debug("[CDS] disabled — skipping archive preparation for {}", config.getServiceName());
            return;
        }

        String cdsArchivePath = resolveCdsArchivePath(config.getServiceName());
        File cdsArchive = new File(cdsArchivePath);

        if (cdsArchive.exists()) {
            log.info("✅ CDS archive already exists for {}: {}", config.getServiceName(), cdsArchivePath);
            return;
        }

        log.info("🏗️ Creating CDS archive for {} (first-time only, takes ~10s)...", config.getServiceName());

        try {
            File archiveDir = cdsArchive.getParentFile();
            if (!archiveDir.exists()) {
                archiveDir.mkdirs();
            }

            List<String> trainingCommand = new ArrayList<>();
            trainingCommand.add("java");

            // Same memory flags as normal start so the archive is consistent
            if (config.getJvmArgs() != null && !config.getJvmArgs().isEmpty()) {
                trainingCommand.addAll(Arrays.asList(config.getJvmArgs().split("\\s+")));
            }
            ensureServiceJvmFloor(config.getServiceName(), trainingCommand);
            appendMemoryOptimizationFlags(trainingCommand);

            // CDS training flags:
            //   ArchiveClassesAtExit  — dump class metadata to archive on JVM exit
            //   spring.context.exit   — Spring Boot 3.3+ exits cleanly after context refresh
            trainingCommand.add("-XX:ArchiveClassesAtExit=" + cdsArchivePath);
            trainingCommand.add("-Dspring.context.exit=onRefresh");

            // Pass same Spring profile and datasource config so context initializes correctly
            if (config.getAppArgs() != null && !config.getAppArgs().isEmpty()) {
                trainingCommand.addAll(Arrays.asList(config.getAppArgs().split("\\s+")));
            }

            trainingCommand.add("-jar");
            trainingCommand.add(jarFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(trainingCommand);
            pb.directory(jarFile.getParentFile());
            pb.redirectErrorStream(true);

            log.info("🏗️ CDS training command: {}", String.join(" ", trainingCommand));
            Process trainingProcess = pb.start();

            // Drain output so the process doesn't block on full pipe buffer
            Thread drainer = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(trainingProcess.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        log.debug("[CDS-train][{}] {}", config.getServiceName(), line);
                    }
                } catch (IOException ignored) {}
            });
            drainer.setDaemon(true);
            drainer.start();

            // Training run should finish in under 60 seconds
            boolean done = trainingProcess.waitFor(60, TimeUnit.SECONDS);
            if (!done) {
                trainingProcess.destroyForcibly();
                log.warn("⚠️ CDS training timed out for {} — skipping archive", config.getServiceName());
                cdsArchive.delete();
                return;
            }

            if (cdsArchive.exists() && cdsArchive.length() > 0) {
                log.info("✅ CDS archive created for {}: {} ({} KB)",
                        config.getServiceName(), cdsArchivePath, cdsArchive.length() / 1024);
            } else {
                log.warn("⚠️ CDS archive not created for {} (exit code: {}). Service will start without CDS.",
                        config.getServiceName(), trainingProcess.exitValue());
            }

        } catch (Exception e) {
            log.warn("⚠️ CDS archive creation failed for {}: {} — continuing without CDS",
                    config.getServiceName(), e.getMessage());
        }
    }

    /**
     * Returns the path where the CDS archive for a service is stored.
     * Archives are kept next to the jar in the target/ directory.
     * Example: D:/project/serverGame/GameServer/user-service/target/user-service-cds.jsa
     */
    public String resolveCdsArchivePath(String serviceName) {
        return baseGameServerPath + "/" + serviceName + "/target/" + serviceName + "-cds.jsa";
    }

    /**
     * Check CDS archive status for all services.
     * Returns a map: serviceName → CDS info (exists, size, jarExists)
     */
    public List<Map<String, Object>> getCdsStatus() {
        List<ServiceConfig> services = configRepository.findAllByStartupOrder();
        List<Map<String, Object>> result = new ArrayList<>();

        for (ServiceConfig config : services) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("serviceName", config.getServiceName());
            entry.put("phase", config.getPhase());

            // Check JAR
            String jarPath = config.getJarPath();
            File jarFile = (jarPath != null && !jarPath.isEmpty())
                    ? new File(jarPath)
                    : new File(baseGameServerPath, config.getServiceName() + "/target/" + config.getServiceName() + "-1.0.0.jar");
            entry.put("jarExists", jarFile.exists());

            // Check archive
            File archive = new File(resolveCdsArchivePath(config.getServiceName()));
            entry.put("archiveExists", archive.exists());
            entry.put("archiveSizeKb", archive.exists() ? archive.length() / 1024 : 0);
            entry.put("archivePath", archive.getAbsolutePath());

            // Staleness check: archive older than jar means JAR was rebuilt → archive needs refresh
            if (archive.exists() && jarFile.exists()) {
                boolean stale = archive.lastModified() < jarFile.lastModified();
                entry.put("stale", stale);
            } else {
                entry.put("stale", false);
            }

            result.add(entry);
        }
        return result;
    }

    /**
     * Delete CDS archive for a single service (use after rebuilding the JAR or after stop).
     * Returns true if deleted, false if it didn't exist or stayed locked.
     */
    public boolean deleteCdsArchive(String serviceName) {
        File archive = new File(resolveCdsArchivePath(serviceName));
        if (!archive.exists()) {
            return false;
        }

        for (int attempt = 1; attempt <= 5; attempt++) {
            if (archive.delete()) {
                log.info("🗑️ CDS archive deleted for {}", serviceName);
                return true;
            }

            log.warn("⚠️ CDS archive still locked for {} (attempt {}/5): {}",
                    serviceName, attempt, archive.getAbsolutePath());
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.warn("⚠️ Failed to delete CDS archive for {} after retries: {}",
                serviceName, archive.getAbsolutePath());
        return false;
    }

    /**
     * Warm CDS archives for all services that have a JAR but no archive yet.
     * Runs training sequentially (not parallel) to avoid overwhelming DB connections.
     * Services with stale archives (JAR newer than archive) are refreshed automatically.
     *
     * NOTE: Training run needs the service's dependencies (DB, Redis) to be running.
     * Services whose training fails are skipped gracefully — they start without CDS.
     */
    @Async
    public CompletableFuture<Map<String, String>> warmAllCdsArchives() {
        log.info("🏗️ Starting CDS warm-up for all services...");
        List<ServiceConfig> services = configRepository.findAllByStartupOrder();
        Map<String, String> results = new LinkedHashMap<>();

        if (!cdsEnabled) {
            log.info("⏭️ CDS warm-up skipped because gameserver.cds.enabled=false");
            for (ServiceConfig config : services) {
                results.put(config.getServiceName(), "DISABLED");
            }
            return CompletableFuture.completedFuture(results);
        }

        for (ServiceConfig config : services) {
            if (!config.getEnabled()) {
                results.put(config.getServiceName(), "SKIPPED_DISABLED");
                continue;
            }

            // Resolve JAR path
            String jarPath = config.getJarPath();
            File jarFile = (jarPath != null && !jarPath.isEmpty())
                    ? new File(jarPath)
                    : new File(baseGameServerPath, config.getServiceName() + "/target/" + config.getServiceName() + "-1.0.0.jar");

            if (!jarFile.exists()) {
                log.warn("⚠️ [CDS warm] JAR not found for {} — skipping", config.getServiceName());
                results.put(config.getServiceName(), "SKIPPED_NO_JAR");
                continue;
            }

            // Delete stale archive (JAR was rebuilt after archive was created)
            File archive = new File(resolveCdsArchivePath(config.getServiceName()));
            if (archive.exists() && archive.lastModified() < jarFile.lastModified()) {
                log.info("🔄 [CDS warm] Stale archive detected for {} — deleting and recreating", config.getServiceName());
                archive.delete();
            }

            if (archive.exists()) {
                log.info("✅ [CDS warm] Archive already up-to-date for {}", config.getServiceName());
                results.put(config.getServiceName(), "ALREADY_EXISTS");
                continue;
            }

            log.info("🏗️ [CDS warm] Training: {} ...", config.getServiceName());
            prepareCdsArchive(config, jarFile);

            File freshArchive = new File(resolveCdsArchivePath(config.getServiceName()));
            if (freshArchive.exists() && freshArchive.length() > 0) {
                results.put(config.getServiceName(), "CREATED_" + freshArchive.length() / 1024 + "KB");
            } else {
                results.put(config.getServiceName(), "FAILED");
            }
        }

        long created  = results.values().stream().filter(v -> v.startsWith("CREATED")).count();
        long failed   = results.values().stream().filter(v -> v.equals("FAILED")).count();
        long skipped  = results.values().stream().filter(v -> v.startsWith("SKIPPED")).count();
        long existing = results.values().stream().filter(v -> v.equals("ALREADY_EXISTS")).count();
        log.info("✅ CDS warm-up complete — created: {}, existing: {}, failed: {}, skipped: {}",
                created, existing, failed, skipped);

        return CompletableFuture.completedFuture(results);
    }

    /**
     * Get DockerManager instance for direct access
     */
    public DockerManager getDockerManager() {
        return dockerManager;
    }
}
