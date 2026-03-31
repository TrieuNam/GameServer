package com.SouthMillion.webSocket_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Login snapshot runtime for R1/R2/R3:
 * - R1: Persist role bootstrap snapshot summary for task/wallet/equip modules.
 * - R2: Compare module versions and report stale modules.
 * - R3: Feature-flag rollout by role hash + p95 bootstrap metric.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSnapshotService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final List<String> TRACKED_MODULES = List.of("task", "wallet", "equip");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${app.login-snapshot.enabled:false}")
    private boolean enabled;

    @Value("${app.login-snapshot.rollout-percent:0}")
    private int rolloutPercent;

    @Value("${app.login-snapshot.ttl-hours:24}")
    private long ttlHours;

    @Value("${app.login-snapshot.schema-version:v1}")
    private String schemaVersion;

    @Value("${app.login-snapshot.module-version-token:bootstrap-v1}")
    private String moduleVersionToken;

    private final AtomicLong assessCalls = new AtomicLong();
    private final AtomicLong rolloutSkipped = new AtomicLong();
    private final AtomicLong snapshotHits = new AtomicLong();
    private final AtomicLong snapshotMiss = new AtomicLong();
    private final AtomicLong snapshotStale = new AtomicLong();
    private final AtomicLong bootstrapWrites = new AtomicLong();

    private final Object metricsLock = new Object();
    private final List<Long> bootstrapDurationsMs = new ArrayList<>();
    private static final int MAX_DURATION_SAMPLES = 2048;

    public SnapshotAssessment assess(Long roleId) {
        assessCalls.incrementAndGet();

        if (!enabled || roleId == null) {
            if (enabled && roleId == null) {
                snapshotMiss.incrementAndGet();
            }
            return SnapshotAssessment.disabled();
        }

        if (!isInRollout(roleId)) {
            rolloutSkipped.incrementAndGet();
            return SnapshotAssessment.rolloutSkipped();
        }

        String snapshotJson = redis.opsForValue().get(snapshotKey(roleId));
        if (snapshotJson == null || snapshotJson.isBlank()) {
            snapshotMiss.incrementAndGet();
            return SnapshotAssessment.miss();
        }

        List<String> staleModules = findStaleModules(roleId);
        if (!staleModules.isEmpty()) {
            snapshotStale.incrementAndGet();
            return SnapshotAssessment.stale(staleModules);
        }

        snapshotHits.incrementAndGet();
        return SnapshotAssessment.hit();
    }

    public void writeBootstrapSnapshot(Long roleId, long totalMs) {
        if (!enabled || roleId == null) {
            return;
        }

        try {
            String now = Instant.now().toString();
            Map<String, Object> modules = new LinkedHashMap<>();
            for (String module : TRACKED_MODULES) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("status", "loaded");
                m.put("version", moduleVersionToken);
                modules.put(module, m);

                redis.opsForValue().set(roleModuleVersionKey(roleId, module), moduleVersionToken, ttlHours, TimeUnit.HOURS);
            }

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("schemaVersion", schemaVersion);
            snapshot.put("updatedAt", now);
            snapshot.put("roleId", roleId);
            snapshot.put("modules", modules);
            snapshot.put("bootstrapMs", totalMs);

            redis.opsForValue().set(snapshotKey(roleId), objectMapper.writeValueAsString(snapshot), ttlHours, TimeUnit.HOURS);

            Map<String, Object> versionSummary = new LinkedHashMap<>();
            versionSummary.put("schemaVersion", schemaVersion);
            versionSummary.put("moduleVersionToken", moduleVersionToken);
            versionSummary.put("updatedAt", now);
            versionSummary.put("modules", modules);
            redis.opsForValue().set(versionKey(roleId), objectMapper.writeValueAsString(versionSummary), ttlHours, TimeUnit.HOURS);

            bootstrapWrites.incrementAndGet();
            recordBootstrapDuration(totalMs);
        } catch (Exception e) {
            log.warn("[login-snapshot] write snapshot failed roleId={} error={}", roleId, e.getMessage());
        }
    }

    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", enabled);
        out.put("rolloutPercent", Math.max(0, Math.min(100, rolloutPercent)));
        out.put("ttlHours", ttlHours);
        out.put("schemaVersion", schemaVersion);
        out.put("moduleVersionToken", moduleVersionToken);
        out.put("assessCalls", assessCalls.get());
        out.put("rolloutSkipped", rolloutSkipped.get());
        out.put("snapshotHits", snapshotHits.get());
        out.put("snapshotMiss", snapshotMiss.get());
        out.put("snapshotStale", snapshotStale.get());
        out.put("bootstrapWrites", bootstrapWrites.get());
        out.put("hitRatio", ratio(snapshotHits.get(), assessCalls.get() - rolloutSkipped.get()));
        out.put("bootstrapP95Ms", bootstrapP95Ms());
        return out;
    }

    public Map<String, Object> roleSnapshotStatus(Long roleId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("roleId", roleId);
        out.put("enabled", enabled);
        out.put("inRollout", roleId != null && isInRollout(roleId));
        out.put("snapshotKey", snapshotKey(roleId));
        out.put("versionKey", versionKey(roleId));
        out.put("snapshotExists", roleId != null && exists(snapshotKey(roleId)));
        out.put("versionExists", roleId != null && exists(versionKey(roleId)));
        out.put("staleModules", roleId == null ? List.of() : findStaleModules(roleId));
        return out;
    }

    private boolean exists(String key) {
        try {
            Boolean ex = redis.hasKey(key);
            return Boolean.TRUE.equals(ex);
        } catch (Exception e) {
            return false;
        }
    }

    private void recordBootstrapDuration(long totalMs) {
        synchronized (metricsLock) {
            bootstrapDurationsMs.add(totalMs);
            if (bootstrapDurationsMs.size() > MAX_DURATION_SAMPLES) {
                bootstrapDurationsMs.remove(0);
            }
        }
    }

    private long bootstrapP95Ms() {
        synchronized (metricsLock) {
            if (bootstrapDurationsMs.isEmpty()) {
                return 0;
            }
            List<Long> sorted = new ArrayList<>(bootstrapDurationsMs);
            sorted.sort(Long::compareTo);
            int idx = (int) Math.ceil(sorted.size() * 0.95) - 1;
            if (idx < 0) idx = 0;
            if (idx >= sorted.size()) idx = sorted.size() - 1;
            return sorted.get(idx);
        }
    }

    private List<String> findStaleModules(Long roleId) {
        List<String> stale = new ArrayList<>();
        for (String module : TRACKED_MODULES) {
            String v = redis.opsForValue().get(roleModuleVersionKey(roleId, module));
            if (v == null || !v.equals(moduleVersionToken)) {
                stale.add(module);
            }
        }
        return stale;
    }

    private boolean isInRollout(Long roleId) {
        int pct = Math.max(0, Math.min(100, rolloutPercent));
        if (pct >= 100) {
            return true;
        }
        if (pct <= 0) {
            return false;
        }
        int bucket = stableBucket(roleId);
        return bucket < pct;
    }

    private int stableBucket(Long roleId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(String.valueOf(roleId).getBytes(StandardCharsets.UTF_8));
            int value = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
            return value % 100;
        } catch (Exception e) {
            return Math.abs(String.valueOf(roleId).hashCode()) % 100;
        }
    }

    private String snapshotKey(Long roleId) {
        return "login:snapshot:" + roleId;
    }

    private String versionKey(Long roleId) {
        return "login:version:" + roleId;
    }

    private String roleModuleVersionKey(Long roleId, String module) {
        return "role:module:version:" + roleId + ":" + module;
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return ((double) numerator) / ((double) denominator);
    }

    @Getter
    @RequiredArgsConstructor
    public static class SnapshotAssessment {
        private final String status;
        private final List<String> staleModules;

        public static SnapshotAssessment hit() {
            return new SnapshotAssessment("HIT", List.of());
        }

        public static SnapshotAssessment miss() {
            return new SnapshotAssessment("MISS", List.of());
        }

        public static SnapshotAssessment stale(List<String> staleModules) {
            return new SnapshotAssessment("STALE", staleModules);
        }

        public static SnapshotAssessment disabled() {
            return new SnapshotAssessment("DISABLED", List.of());
        }

        public static SnapshotAssessment rolloutSkipped() {
            return new SnapshotAssessment("ROLLOUT_SKIPPED", List.of());
        }
    }
}
