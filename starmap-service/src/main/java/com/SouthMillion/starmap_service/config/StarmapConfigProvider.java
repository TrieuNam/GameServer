package com.SouthMillion.starmap_service.config;

import com.SouthMillion.starmap_service.client.ConfigServiceClient;
import com.SouthMillion.starmap_service.model.config.StarmapConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Provider for starmap configuration with Redis-first caching strategy
 * Pattern: Redis → config-service → fallback null
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StarmapConfigProvider {

    private final ConfigServiceClient configServiceClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${starmap.config.path:gameworld/starmap/starmap.json}")
    private String configPath;

    @Value("${starmap.config.redis-enabled:true}")
    private boolean redisEnabled;

    @Value("${starmap.config.redis-ttl-hours:24}")
    private long redisTtlHours;

    @Value("${starmap.config.refresh-interval-ms:3600000}") // 1 hour default
    private long refreshIntervalMs;

    private static final String REDIS_KEY_PREFIX = "cfg:file:";
    private static final String ETAG_SUFFIX = ":etag";

    private String lastETag;

    /**
     * Warmup cache on startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing StarmapConfigProvider with path: {}", configPath);
        try {
            getStarmapConfig();
            log.info("StarmapConfigProvider initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to warmup starmap config cache: {}", e.getMessage());
        }
    }

    /**
     * Scheduled refresh of configuration cache
     */
    @Scheduled(fixedDelayString = "${starmap.config.refresh-interval-ms:3600000}")
    public void scheduledRefresh() {
        log.debug("Running scheduled starmap config refresh");
        try {
            refreshFromConfigService();
        } catch (Exception e) {
            log.error("Scheduled config refresh failed: {}", e.getMessage());
        }
    }

    /**
     * Get starmap configuration with Redis-first caching
     *
     * @return StarmapConfig or null if not available
     */
    public StarmapConfig getStarmapConfig() {
        // 1. Try Redis cache first
        if (redisEnabled) {
            try {
                StarmapConfig cached = getFromRedis();
                if (cached != null) {
                    log.debug("Starmap config loaded from Redis cache");
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Failed to load from Redis: {}", e.getMessage());
            }
        }

        // 2. Fallback to config-service
        try {
            StarmapConfig config = refreshFromConfigService();
            if (config != null) {
                log.info("Starmap config loaded from config-service");
                return config;
            }
        } catch (Exception e) {
            log.error("Failed to load from config-service: {}", e.getMessage());
        }

        // 3. Return null (caller should use defaults)
        log.warn("Starmap config not available, caller should use defaults");
        return null;
    }

    /**
     * Load configuration from Redis cache
     */
    private StarmapConfig getFromRedis() {
        String redisKey = getRedisKey();
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, StarmapConfig.class);
        } catch (Exception e) {
            log.error("Failed to deserialize starmap config from Redis: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Refresh configuration from config-service and update Redis cache
     */
    private StarmapConfig refreshFromConfigService() {
        try {
            // Use ETag for conditional GET
            ResponseEntity<byte[]> response = configServiceClient.getFile(configPath, lastETag);

            // 304 Not Modified - use cached version
            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                log.debug("Config not modified (ETag match), using cache");
                return getFromRedis();
            }

            // 200 OK - new content available
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                byte[] content = response.getBody();
                String json = new String(content, StandardCharsets.UTF_8);

                // Parse JSON
                StarmapConfig config = objectMapper.readValue(json, StarmapConfig.class);

                // Update ETag
                String newETag = response.getHeaders().getETag();
                if (newETag != null) {
                    lastETag = newETag;
                }

                // Cache in Redis
                if (redisEnabled) {
                    cacheInRedis(json, newETag);
                }

                log.info("Starmap config refreshed from config-service");
                return config;
            }

            log.warn("Unexpected response from config-service: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error loading config from config-service: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Cache configuration in Redis
     */
    private void cacheInRedis(String json, String etag) {
        try {
            String redisKey = getRedisKey();

            // Store config JSON
            redisTemplate.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);

            // Store ETag if available
            if (etag != null) {
                redisTemplate.opsForValue().set(
                    redisKey + ETAG_SUFFIX,
                    etag,
                    redisTtlHours,
                    TimeUnit.HOURS
                );
            }

            log.debug("Cached starmap config in Redis with TTL {}h", redisTtlHours);
        } catch (Exception e) {
            log.error("Failed to cache in Redis: {}", e.getMessage());
        }
    }

    /**
     * Get Redis key for starmap config
     */
    private String getRedisKey() {
        // Convert path to Redis key format: gameworld/starmap/starmap.json -> gameworld:starmap:starmap.json
        return REDIS_KEY_PREFIX + configPath.replace("/", ":");
    }

    /**
     * Force refresh configuration from config-service
     */
    public void forceRefresh() {
        log.info("Force refreshing starmap config");
        lastETag = null; // Clear ETag to force full reload
        refreshFromConfigService();
    }
}
