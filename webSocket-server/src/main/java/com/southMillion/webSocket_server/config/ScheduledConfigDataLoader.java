package com.SouthMillion.webSocket_server.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.service.client.ConfigFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduled loader for config JSON files from config-service into Redis.
 *
 * Purpose:
 * - Periodically refresh static config data to ensure up-to-date data is available
 * - Pre-load data before login to avoid calling config-service during peak times
 * - Maintains Redis cache with TTL to prevent stale data
 *
 * Schedule:
 * - Runs every N minutes (configurable via app.config-loader.schedule-interval-minutes)
 * - Default: 30 minutes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledConfigDataLoader {

    private final ConfigFeign configFeign;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${app.config-loader.enabled:true}")
    private boolean enabled;

    @Value("${app.config-loader.schedule-interval-minutes:30}")
    private long scheduleIntervalMinutes;

    @Value("${app.config-loader.ttl-hours:24}")
    private long ttlHours;

    @Value("${app.config-loader.paths:gameworld/logicconfig/task_cfg.json,gameworld/logicconfig/roleexp.json,gameworld/logicconfig/role_name.json,gameworld/skill/single_skill.json,gameworld/skill/passive_skill.json,gameworld/item/equipment.json,gameworld/item/other.json,gameworld/item/expense.json,gameworld/item/gift.json,gameworld/logicconfig/shop_cfg.json}")
    private List<String> configPaths;

    private final AtomicLong lastRunTimestamp = new AtomicLong(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * Scheduled task to load config data from config-service into Redis.
     * Runs every N minutes based on schedule-interval-minutes configuration.
     *
     * fixedDelayString uses SpEL to convert minutes to milliseconds
     *
     * Public method to allow manual triggering via REST endpoint
     */
    @Scheduled(fixedDelayString = "#{${app.config-loader.schedule-interval-minutes:30} * 60 * 1000}",
               initialDelayString = "#{${app.config-loader.initial-delay-minutes:5} * 60 * 1000}")
    public void loadConfigData() {
        if (!enabled) {
            log.debug("[scheduled-config-loader] disabled, skipping");
            return;
        }

        long startTime = System.currentTimeMillis();
        lastRunTimestamp.set(startTime);

        log.info("[scheduled-config-loader] Starting scheduled config data load, interval={}min, paths={}",
                scheduleIntervalMinutes, configPaths.size());

        List<String> paths = sanitizePaths(configPaths);
        int successTotal = 0;
        int failureTotal = 0;

        for (String path : paths) {
            try {
                boolean loaded = loadConfigFile(path);
                if (loaded) {
                    successTotal++;
                } else {
                    failureTotal++;
                }
            } catch (Exception e) {
                log.warn("[scheduled-config-loader] Failed to load path={}: {}", path, e.getMessage());
                failureTotal++;
            }
        }

        successCount.set(successTotal);
        failureCount.set(failureTotal);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[scheduled-config-loader] Completed in {}ms, success={}, failure={}",
                elapsed, successTotal, failureTotal);
    }

    /**
     * Load a single config file from config-service and store in Redis.
     *
     * @param path config file path (e.g., "gameworld/logicconfig/task_cfg.json")
     * @return true if loaded successfully, false otherwise
     */
    private boolean loadConfigFile(String path) {
        long startTime = System.currentTimeMillis();

        try {
            // Check if data already exists in Redis
            String redisKey = fileKey(path);
            String existingData = redis.opsForValue().get(redisKey);

            // Fetch from config-service
            ResponseEntity<byte[]> resp = configFeign.getFile(path, null);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("[scheduled-config-loader] Failed to fetch path={}, status={}",
                        path, resp.getStatusCode());
                return false;
            }

            String json = new String(resp.getBody(), StandardCharsets.UTF_8);
            String hash = sha256Hex(json);
            String loadedAt = Instant.now().toString();

            // Store in Redis with TTL
            redis.opsForValue().set(redisKey, json, ttlHours, TimeUnit.HOURS);
            redis.opsForValue().set(fileMetaHashKey(path), hash, ttlHours, TimeUnit.HOURS);
            redis.opsForValue().set(fileMetaLoadedAtKey(path), loadedAt, ttlHours, TimeUnit.HOURS);

            // Build task condition index if loading task_cfg.json
            if ("gameworld/logicconfig/task_cfg.json".equals(path)) {
                buildTaskConditionIndex(json);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            boolean wasUpdated = existingData == null || !existingData.equals(json);

            log.info("[scheduled-config-loader] Loaded path={}, elapsed={}ms, updated={}, hash={}",
                    path, elapsed, wasUpdated, hash.substring(0, 8));

            return true;
        } catch (Exception e) {
            log.warn("[scheduled-config-loader] Error loading path={}: {}", path, e.getMessage());
            return false;
        }
    }

    /**
     * Build task condition index for fast O(1) condition lookup.
     * Same logic as StartupConfigRedisPreloader.
     */
    private void buildTaskConditionIndex(String taskCfgJson) {
        try {
            JsonNode root = objectMapper.readTree(taskCfgJson);
            JsonNode taskList = root.get("task_list");
            if (taskList == null || !taskList.isArray()) {
                log.warn("[scheduled-config-loader] task_cfg has no task_list array");
                return;
            }

            Map<String, List<Map<String, Object>>> byCondition = new HashMap<>();
            for (JsonNode node : taskList) {
                String condition = node.path("condition").asText("").trim();
                if (condition.isEmpty()) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("taskId", node.path("task_id").asText(""));
                row.put("nextTaskId", node.path("next_task_id").asText(""));
                row.put("param", node.path("param").asText("0"));
                row.put("param1", node.path("param_1").asText("0"));
                row.put("reset", node.path("reset").asText("0"));
                byCondition.computeIfAbsent(condition, k -> new ArrayList<>()).add(row);
            }

            for (Map.Entry<String, List<Map<String, Object>>> e : byCondition.entrySet()) {
                String condition = e.getKey();
                String key = "cfg:task:condition:" + condition;
                String val = objectMapper.writeValueAsString(e.getValue());
                redis.opsForValue().set(key, val, ttlHours, TimeUnit.HOURS);
            }

            Map<String, Object> meta = new HashMap<>();
            meta.put("conditionCount", byCondition.size());
            meta.put("updatedAt", Instant.now().toString());
            redis.opsForValue().set("cfg:task:meta:version",
                    objectMapper.writeValueAsString(meta), ttlHours, TimeUnit.HOURS);

            log.info("[scheduled-config-loader] Task condition index built, conditionCount={}",
                    byCondition.size());
        } catch (Exception e) {
            log.warn("[scheduled-config-loader] Task condition index build failed: {}", e.getMessage());
        }
    }

    /**
     * Get current status for monitoring/health check.
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("scheduleIntervalMinutes", scheduleIntervalMinutes);
        status.put("ttlHours", ttlHours);
        status.put("configPathsCount", configPaths.size());
        status.put("lastRunTimestamp", lastRunTimestamp.get());
        status.put("lastRunTime", lastRunTimestamp.get() > 0
                ? Instant.ofEpochMilli(lastRunTimestamp.get()).toString()
                : "Never");
        status.put("successCount", successCount.get());
        status.put("failureCount", failureCount.get());
        return status;
    }

    /**
     * Check if required config data is available in Redis.
     * Called during login to verify data exists before proceeding.
     */
    public boolean isConfigDataAvailable() {
        if (!enabled) {
            return true; // If disabled, assume data will be fetched on-demand
        }

        // Check a few critical paths to verify data is loaded
        List<String> criticalPaths = List.of(
                "gameworld/logicconfig/task_cfg.json",
                "gameworld/logicconfig/roleexp.json"
        );

        for (String path : criticalPaths) {
            String redisKey = fileKey(path);
            String data = redis.opsForValue().get(redisKey);
            if (data == null || data.isBlank()) {
                log.warn("[scheduled-config-loader] Critical config data missing in Redis: {}", path);
                return false;
            }
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper methods (same as StartupConfigRedisPreloader)
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> sanitizePaths(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        Set<String> dedupe = new LinkedHashSet<>();
        for (String path : raw) {
            if (path == null) {
                continue;
            }
            String p = path.trim();
            if (!p.isEmpty()) {
                dedupe.add(p);
            }
        }
        out.addAll(dedupe);
        return out;
    }

    private String fileKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    private String fileMetaHashKey(String path) {
        return "cfg:file:meta:hash:" + path.replace('/', ':');
    }

    private String fileMetaLoadedAtKey(String path) {
        return "cfg:file:meta:loadedAt:" + path.replace('/', ':');
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
