package com.SouthMillion.webSocket_server.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.service.ConfigSnapshotLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Preload static config JSON files from remote config-service into Redis after websocket-server startup.
 *
 * Purpose:
 * - Warm Redis once at startup so login paths can reuse cached config data.
 * - Reduce repeated config-service calls during login peak.
 * - Build task condition index from task_cfg.json for O(1) condition lookup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConfigRedisPreloader {

    private final ConfigSnapshotLookupService configLookup;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${app.redis-preload.enabled:true}")
    private boolean enabled;

    @Value("${app.redis-preload.ttl-hours:24}")
    private long ttlHours;

    @Value("${app.redis-preload.p0-paths:gameworld/logicconfig/task_cfg.json,gameworld/logicconfig/roleexp.json,gameworld/logicconfig/role_name.json,gameworld/skill/single_skill.json,gameworld/skill/passive_skill.json}")
    private List<String> p0Paths;

    @Value("${app.redis-preload.p1-paths:gameworld/item/equipment.json,gameworld/item/other.json,gameworld/item/expense.json,gameworld/item/gift.json,gameworld/logicconfig/shop_cfg.json,gameworld/logicconfig/shop_shenmi.json,gameworld/logicconfig/cloth_shop.json,gameworld/logicconfig/model_clothes.json,gameworld/logicconfig/angel.json,gameworld/logicconfig/unpack.json,gameworld/logicconfig/kaixiangdaji.json}")
    private List<String> p1Paths;

    @Value("${app.redis-preload.p1-async:true}")
    private boolean p1Async;

    @Value("${app.redis-preload.p1-timeout-ms:15000}")
    private long p1TimeoutMs;

    @Value("${app.redis-preload.fetch-attempts:3}")
    private int fetchAttempts;

    @Value("${app.redis-preload.fetch-backoff-ms:1000}")
    private long fetchBackoffMs;

    private final ConcurrentHashMap<String, Map<String, Object>> fileStatuses = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Map<String, Object> summary = Map.of(
        "enabled", true,
        "state", "INIT",
        "startedAt", "",
        "completedAt", "",
        "ok", 0,
        "fail", 0,
        "p1State", "PENDING"
    );

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reloadNow();
    }

    public boolean reloadNow() {
        if (!enabled) {
            log.info("[redis-preload] disabled");
            summary = Map.of(
                "enabled", false,
                "state", "DISABLED",
                "startedAt", Instant.now().toString(),
                "completedAt", Instant.now().toString(),
                "ok", 0,
                "fail", 0,
                "p1State", "DISABLED"
            );
            return true;
        }

        if (!running.compareAndSet(false, true)) {
            log.info("[redis-preload] already running; skip duplicate trigger");
            return isReadyForLogin();
        }

        long startedAt = System.currentTimeMillis();
        String startedAtIso = Instant.now().toString();

        summary = new HashMap<>();
        summary.put("enabled", true);
        summary.put("state", "RUNNING");
        summary.put("startedAt", startedAtIso);
        summary.put("completedAt", "");
        summary.put("ok", 0);
        summary.put("fail", 0);
        summary.put("p1State", p1Async ? "RUNNING_ASYNC" : "RUNNING_SYNC");
        summary.put("p1TimeoutMs", p1TimeoutMs);

        List<String> p0 = sanitizePaths(p0Paths);
        List<String> p1 = sanitizePaths(p1Paths);

        log.info("[redis-preload] start, ttlHours={}, p0={}, p1={}, p1Async={}, p1TimeoutMs={}",
            ttlHours, p0.size(), p1.size(), p1Async, p1TimeoutMs);

        TierResult p0Result = preloadTier("P0", p0);

        int ok = p0Result.ok;
        int fail = p0Result.fail;

        try {
            if (p1Async) {
                ExecutorService es = Executors.newSingleThreadExecutor();
                Future<TierResult> p1Future = es.submit(() -> preloadTier("P1", p1));
                try {
                    TierResult p1Result = p1Future.get(p1TimeoutMs, TimeUnit.MILLISECONDS);
                    ok += p1Result.ok;
                    fail += p1Result.fail;
                    updateSummary(ok, fail, "COMPLETED", "COMPLETED", startedAtIso);
                } catch (TimeoutException e) {
                    p1Future.cancel(true);
                    updateSummary(ok, fail, "PARTIAL_TIMEOUT", "TIMEOUT", startedAtIso);
                    log.warn("[redis-preload] P1 timeout after {}ms", p1TimeoutMs);
                } catch (Exception e) {
                    updateSummary(ok, fail + 1, "PARTIAL_ERROR", "ERROR", startedAtIso);
                    log.warn("[redis-preload] P1 failed: {}", e.getMessage());
                } finally {
                    es.shutdownNow();
                }
            } else {
                TierResult p1Result = preloadTier("P1", p1);
                ok += p1Result.ok;
                fail += p1Result.fail;
                updateSummary(ok, fail, "COMPLETED", "COMPLETED", startedAtIso);
            }

            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("[redis-preload] completed in {}ms, ok={}, fail={}", elapsed, ok, fail);
            return fail == 0;
        } finally {
            running.set(false);
        }
    }

    public boolean isReadyForLogin() {
        if (!enabled) {
            return true;
        }
        Object state = summary.get("state");
        Object fail = summary.get("fail");
        long failCount = fail instanceof Number n ? n.longValue() : Long.MAX_VALUE;
        return "COMPLETED".equals(String.valueOf(state)) && failCount == 0;
    }

    public Map<String, Object> statusSnapshot() {
        Map<String, Object> out = new HashMap<>();
        out.put("summary", summary);
        out.put("files", new HashMap<>(fileStatuses));
        return out;
    }

    private void updateSummary(int ok, int fail, String state, String p1State, String startedAtIso) {
        Map<String, Object> next = new HashMap<>();
        next.put("enabled", true);
        next.put("state", state);
        next.put("startedAt", startedAtIso);
        next.put("completedAt", Instant.now().toString());
        next.put("ok", ok);
        next.put("fail", fail);
        next.put("p1State", p1State);
        next.put("p1TimeoutMs", p1TimeoutMs);
        summary = next;
    }

    private TierResult preloadTier(String tier, List<String> paths) {
        int ok = 0;
        int fail = 0;
        for (String path : paths) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("[redis-preload] {} interrupted", tier);
                break;
            }
            boolean done = preloadOne(path, tier);
            if (done) {
                ok++;
            } else {
                fail++;
            }
        }
        return new TierResult(ok, fail);
    }

    private boolean preloadOne(String path, String tier) {
        long startedAt = System.currentTimeMillis();
        Map<String, Object> status = new HashMap<>();
        status.put("tier", tier);
        status.put("path", path);
        status.put("ok", false);
        status.put("status", "INIT");
        status.put("error", "");
        status.put("loadedAt", "");
        status.put("durationMs", 0);
        status.put("hash", "");
        status.put("source", "");
        try {
            ConfigSnapshotLookupService.LookupResult lookup = loadFromConfigServiceWithRetry(path);
            String json = lookup.payload();
            String fileKey = fileKey(path);
            String hash = sha256Hex(json);
            String loadedAt = Instant.now().toString();

            status.put("source", lookup.source());

            redis.opsForValue().set(fileKey, json, ttlHours, TimeUnit.HOURS);
            redis.opsForValue().set(fileMetaHashKey(path), hash, ttlHours, TimeUnit.HOURS);
            redis.opsForValue().set(fileMetaLoadedAtKey(path), loadedAt, ttlHours, TimeUnit.HOURS);

            if ("gameworld/logicconfig/task_cfg.json".equals(path)) {
                buildTaskConditionIndex(json);
            }

            status.put("ok", true);
            status.put("status", "OK");
            status.put("loadedAt", loadedAt);
            status.put("durationMs", System.currentTimeMillis() - startedAt);
            status.put("hash", hash);
            fileStatuses.put(path, status);

            return true;
        } catch (Exception e) {
            log.warn("[redis-preload] failed path={} error={}", path, e.getMessage());
            status.put("status", "ERROR");
            status.put("error", e.getMessage() == null ? "" : e.getMessage());
            status.put("durationMs", System.currentTimeMillis() - startedAt);
            fileStatuses.put(path, status);
            return false;
        }
    }

    private ConfigSnapshotLookupService.LookupResult loadFromConfigServiceWithRetry(String path) {
        RuntimeException last = null;
        int maxAttempts = Math.max(1, fetchAttempts);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return configLookup.warmFromRemote(path);
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt < maxAttempts) {
                    long delay = Math.max(100L, fetchBackoffMs * attempt);
                    log.warn("[redis-preload] fetch retry path={} attempt={}/{} delayMs={} error={}",
                            path, attempt, maxAttempts, delay, ex.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }
        throw last == null ? new IllegalStateException("Failed to preload config path=" + path) : last;
    }

    private void buildTaskConditionIndex(String taskCfgJson) {
        try {
            JsonNode root = objectMapper.readTree(taskCfgJson);
            JsonNode taskList = root.get("task_list");
            if (taskList == null || !taskList.isArray()) {
                log.warn("[redis-preload] task_cfg has no task_list array");
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
            redis.opsForValue().set("cfg:task:meta:version", objectMapper.writeValueAsString(meta), ttlHours, TimeUnit.HOURS);

            log.info("[redis-preload] task condition index built, conditionCount={}", byCondition.size());
        } catch (Exception e) {
            log.warn("[redis-preload] task condition index failed: {}", e.getMessage());
        }
    }

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

    private static class TierResult {
        final int ok;
        final int fail;

        TierResult(int ok, int fail) {
            this.ok = ok;
            this.fail = fail;
        }
    }
}
