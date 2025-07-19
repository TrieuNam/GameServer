package com.SouthMillion.config_service.controller;

import com.SouthMillion.config_service.service.ConfigDynamicService;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConfigController {
    private final ConfigDynamicService configService;

    /** Lấy file config lẻ (ví dụ: /api/config/task_cfg) */
    @GetMapping("/{fileName}")
    public ResponseEntity<JsonNode> getConfigFile(@PathVariable String fileName) {
        JsonNode config = configService.getConfig(fileName);
        if (config == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(config);
    }

    @GetMapping("/dev-query-h02")
    public ResponseEntity<JsonNode> getDevQueryConfig(
            @RequestParam(value = "plat", required = false) String plat,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "pkg", required = false) String pkg,
            @RequestParam(value = "device", required = false) String device
    ) {
        // Bạn có thể sử dụng các biến plat, version, pkg, device ở đây nếu muốn trả về khác nhau theo param
        // Ví dụ: Logging hoặc chọn file động theo device/version/plat...

        JsonNode config = configService.getConfig("dev-query-h02");
        if (config == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(config);
    }

    /** API nhóm */
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, JsonNode>> getAllTasks() {
        return ResponseEntity.ok(configService.getAllTasks());
    }
    @GetMapping("/rewards")
    public ResponseEntity<Map<String, JsonNode>> getAllRewards() {
        return ResponseEntity.ok(configService.getAllRewards());
    }
    @GetMapping("/dungeons")
    public ResponseEntity<Map<String, JsonNode>> getAllDungeons() {
        return ResponseEntity.ok(configService.getAllDungeons());
    }
    @GetMapping("/system")
    public ResponseEntity<Map<String, JsonNode>> getAllSystems() {
        return ResponseEntity.ok(configService.getAllSystems());
    }
    @GetMapping("/audio-log")
    public ResponseEntity<Map<String, JsonNode>> getAllAudioLog() {
        return ResponseEntity.ok(configService.getAllAudioLog());
    }
    @GetMapping("/shops")
    public ResponseEntity<Map<String, JsonNode>> getAllShops() {
        return ResponseEntity.ok(configService.getAllShops());
    }

    /** Test preload group (Virtual Thread) */
    @PostMapping("/preload-tasks")
    public ResponseEntity<String> preloadTasks() throws ExecutionException, InterruptedException {
        configService.preloadTasksAsync().get();
        return ResponseEntity.ok("Preloaded tasks using virtual thread.");
    }

    /** Test preload reward (Virtual Thread) */
    @PostMapping("/preload-rewards")
    public ResponseEntity<String> preloadRewards() throws ExecutionException, InterruptedException {
        configService.preloadRewardsAsync().get();
        return ResponseEntity.ok("Preloaded rewards using virtual thread.");
    }

    /** Test clear cache (Platform Thread) */
    @PostMapping("/clear-cache")
    public ResponseEntity<String> clearCache() {
        configService.clearConfigCache();
        return ResponseEntity.ok("Clear cache task triggered (platform thread).");
    }

    /** Test task compute nặng (Platform Thread) */
    @PostMapping("/heavy-compute")
    public ResponseEntity<String> heavyCompute() {
        configService.runHeavyCompute();
        return ResponseEntity.ok("Heavy compute task started (platform thread).");
    }
}