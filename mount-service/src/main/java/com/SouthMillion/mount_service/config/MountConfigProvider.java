package com.SouthMillion.mount_service.config;

import com.SouthMillion.mount_service.client.ConfigServiceClient;
import com.SouthMillion.mount_service.model.config.HarnessConfig;
import com.SouthMillion.mount_service.model.config.MountConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mount configuration provider with Redis-first lookup strategy
 * Pattern: Redis cache → config-service → fallback defaults
 *
 * Strategy:
 * 1. Check Redis cache first
 * 2. If miss, call config-service
 * 3. Cache result in Redis for future requests
 * 4. Fall back to hardcoded defaults if all else fails
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MountConfigProvider {

    private final ConfigServiceClient configServiceClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;

    @Value("${mount.config.path:gameworld/mount/mount.json}")
    private String mountConfigPath;

    @Value("${mount.harness.config.path:gameworld/mount/harness.json}")
    private String harnessConfigPath;

    @Value("${mount.config.redis-enabled:true}")
    private boolean redisEnabled;

    @Value("${mount.config.redis-ttl-hours:24}")
    private long redisTtlHours;

    // Cache references
    private final AtomicReference<MountConfig> mountConfigRef = new AtomicReference<>();
    private final AtomicReference<HarnessConfig> harnessConfigRef = new AtomicReference<>();

    // ETag tracking for conditional GET
    private final AtomicReference<String> mountEtagRef = new AtomicReference<>();
    private final AtomicReference<String> harnessEtagRef = new AtomicReference<>();

    // Status tracking
    private final AtomicReference<Instant> lastRefreshAtRef = new AtomicReference<>();
    private final AtomicReference<Instant> lastSuccessAtRef = new AtomicReference<>();
    private final AtomicReference<String> lastSourceRef = new AtomicReference<>("none");
    private final AtomicReference<String> lastErrorRef = new AtomicReference<>();

    // Guard: Prevent duplicate warmup from multiple context refreshes
    private final AtomicBoolean warmedUp = new AtomicBoolean(false);

    /**
     * Warm up cache on application startup
     */
    @EventListener(ContextRefreshedEvent.class)
    public void warmUp() {
        if (warmedUp.compareAndSet(false, true)) {
            log.info("[MountConfigProvider] Warming up mount configurations...");
            refreshFromRemote(false);
        }
    }

    /**
     * Scheduled refresh every hour
     */
    @Scheduled(
        initialDelayString = "${mount.config.refresh-initial-delay-ms:60000}",
        fixedDelayString = "${mount.config.refresh-interval-ms:3600000}"
    )
    public void scheduledRefresh() {
        refreshFromRemote(false);
    }

    /**
     * Manual reload (bypasses cache)
     */
    public void manualReload() {
        log.info("[MountConfigProvider] Manual reload triggered");
        refreshFromRemote(true);
    }

    /**
     * Get current mount configuration
     */
    public MountConfig getMountConfig() {
        MountConfig config = mountConfigRef.get();
        if (config == null) {
            log.warn("[MountConfigProvider] MountConfig not loaded, returning empty config");
            return new MountConfig();
        }
        return config;
    }

    /**
     * Get current harness configuration
     */
    public HarnessConfig getHarnessConfig() {
        HarnessConfig config = harnessConfigRef.get();
        if (config == null) {
            log.warn("[MountConfigProvider] HarnessConfig not loaded, returning empty config");
            return new HarnessConfig();
        }
        return config;
    }

    /**
     * Check if configurations are loaded
     */
    public boolean isConfigLoaded() {
        return mountConfigRef.get() != null && harnessConfigRef.get() != null;
    }

    /**
     * Refresh configurations from remote sources
     */
    private void refreshFromRemote(boolean force) {
        lastRefreshAtRef.set(Instant.now());

        try {
            // Load mount config
            loadMountConfig(force);

            // Load harness config
            loadHarnessConfig(force);

            lastSuccessAtRef.set(Instant.now());
            lastErrorRef.set(null);

            log.info("[MountConfigProvider] Successfully loaded mount configurations");
        } catch (Exception e) {
            log.error("[MountConfigProvider] Failed to refresh configs: {}", e.getMessage(), e);
            lastErrorRef.set(e.getMessage());
        }
    }

    /**
     * Load mount configuration
     */
    private void loadMountConfig(boolean force) {
        String redisKey = toRedisKey(mountConfigPath);

        // 1. Try Redis first (if enabled and not force reload)
        if (redisEnabled && !force) {
            String cached = redis.opsForValue().get(redisKey);
            if (cached != null && !cached.isBlank()) {
                log.debug("[MountConfigProvider] Redis HIT for mount config");
                try {
                    MountConfig config = objectMapper.readValue(cached, MountConfig.class);
                    mountConfigRef.set(config);
                    lastSourceRef.set("redis");
                    touchRedisKey(redisKey);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to parse cached mount config from Redis: {}", e.getMessage());
                    redis.delete(redisKey);
                }
            }
            log.debug("[MountConfigProvider] Redis MISS for mount config");
        }

        // 2. Call config-service
        try {
            String ifNoneMatch = force ? null : mountEtagRef.get();
            ResponseEntity<byte[]> response = configServiceClient.getFile(mountConfigPath, ifNoneMatch);

            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                log.debug("[MountConfigProvider] Mount config not modified (304)");
                lastSourceRef.set("cache");
                return;
            }

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Mount config refresh skipped, status={}", response.getStatusCode());
                return;
            }

            String payload = new String(response.getBody(), StandardCharsets.UTF_8);
            MountConfig config = objectMapper.readValue(payload, MountConfig.class);

            mountConfigRef.set(config);

            // Update ETag
            String etag = response.getHeaders().getFirst(HttpHeaders.ETAG);
            if (StringUtils.hasText(etag)) {
                mountEtagRef.set(etag);
            }

            lastSourceRef.set("config-service");

            // 3. Cache in Redis
            if (redisEnabled) {
                redis.opsForValue().set(redisKey, payload, redisTtlHours, TimeUnit.HOURS);
                log.debug("[MountConfigProvider] Cached mount config in Redis");
            }

            log.info("Loaded mount config from config-service (path={})", mountConfigPath);
        } catch (FeignException.NotFound ex) {
            log.warn("Mount config path not found on config-service: {}", mountConfigPath);
        } catch (FeignException ex) {
            if (ex.status() == 304) {
                lastSourceRef.set("cache");
                return;
            }
            log.warn("Mount config refresh failed via Feign, status={}", ex.status());
        } catch (Exception ex) {
            log.warn("Mount config refresh failed: {}", ex.getMessage());
        }
    }

    /**
     * Load harness configuration
     */
    private void loadHarnessConfig(boolean force) {
        String redisKey = toRedisKey(harnessConfigPath);

        // 1. Try Redis first (if enabled and not force reload)
        if (redisEnabled && !force) {
            String cached = redis.opsForValue().get(redisKey);
            if (cached != null && !cached.isBlank()) {
                log.debug("[MountConfigProvider] Redis HIT for harness config");
                try {
                    HarnessConfig config = objectMapper.readValue(cached, HarnessConfig.class);
                    harnessConfigRef.set(config);
                    touchRedisKey(redisKey);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to parse cached harness config from Redis: {}", e.getMessage());
                    redis.delete(redisKey);
                }
            }
            log.debug("[MountConfigProvider] Redis MISS for harness config");
        }

        // 2. Call config-service
        try {
            String ifNoneMatch = force ? null : harnessEtagRef.get();
            ResponseEntity<byte[]> response = configServiceClient.getFile(harnessConfigPath, ifNoneMatch);

            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                log.debug("[MountConfigProvider] Harness config not modified (304)");
                return;
            }

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Harness config refresh skipped, status={}", response.getStatusCode());
                return;
            }

            String payload = new String(response.getBody(), StandardCharsets.UTF_8);
            HarnessConfig config = objectMapper.readValue(payload, HarnessConfig.class);

            harnessConfigRef.set(config);

            // Update ETag
            String etag = response.getHeaders().getFirst(HttpHeaders.ETAG);
            if (StringUtils.hasText(etag)) {
                harnessEtagRef.set(etag);
            }

            // 3. Cache in Redis
            if (redisEnabled) {
                redis.opsForValue().set(redisKey, payload, redisTtlHours, TimeUnit.HOURS);
                log.debug("[MountConfigProvider] Cached harness config in Redis");
            }

            log.info("Loaded harness config from config-service (path={})", harnessConfigPath);
        } catch (FeignException.NotFound ex) {
            log.warn("Harness config path not found on config-service: {}", harnessConfigPath);
        } catch (FeignException ex) {
            if (ex.status() == 304) {
                return;
            }
            log.warn("Harness config refresh failed via Feign, status={}", ex.status());
        } catch (Exception ex) {
            log.warn("Harness config refresh failed: {}", ex.getMessage());
        }
    }

    /**
     * Touch Redis key to extend TTL
     */
    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[MountConfigProvider] Redis TTL touch failed key={}", redisKey);
        }
    }

    /**
     * Convert file path to Redis key
     * Pattern: cfg:file:{path-with-colon}
     */
    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    /**
     * Get status information
     */
    public ConfigStatus getStatus() {
        return new ConfigStatus(
            mountConfigPath,
            harnessConfigPath,
            mountConfigRef.get() != null,
            harnessConfigRef.get() != null,
            lastRefreshAtRef.get(),
            lastSuccessAtRef.get(),
            lastSourceRef.get(),
            lastErrorRef.get()
        );
    }

    public record ConfigStatus(
        String mountConfigPath,
        String harnessConfigPath,
        boolean mountConfigLoaded,
        boolean harnessConfigLoaded,
        Instant lastRefreshAt,
        Instant lastSuccessAt,
        String source,
        String lastError
    ) {}
}
