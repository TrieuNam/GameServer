package com.SouthMillion.rune_service.config;

import com.SouthMillion.rune_service.client.ConfigServiceClient;
import com.SouthMillion.rune_service.model.config.RuneConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Rune Configuration Provider
 * Loads rune.json from config-service with Redis-first caching strategy
 * Pattern: Redis → config-service → fallback defaults
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuneConfigProvider {

    private final ConfigServiceClient configServiceClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rune.config.path:gameworld/rune/rune.json}")
    private String runeConfigPath;

    @Value("${rune.config.redis-enabled:true}")
    private boolean redisEnabled;

    @Value("${rune.config.redis-ttl-hours:24}")
    private int redisTtlHours;

    @Value("${rune.config.refresh-interval-ms:3600000}")
    private long refreshIntervalMs;

    private static final String REDIS_KEY_PREFIX = "cfg:file:";

    private RuneConfig cachedRuneConfig;
    private String lastETag;

    /**
     * Warmup: Load config on startup
     */
    @PostConstruct
    public void warmup() {
        log.info("Warming up rune config provider...");
        try {
            loadRuneConfig();
            log.info("Rune config warmed up successfully");
        } catch (Exception e) {
            log.warn("Failed to warmup rune config, will use defaults: {}", e.getMessage());
        }
    }

    /**
     * Scheduled refresh every hour (configurable)
     */
    @Scheduled(fixedDelayString = "${rune.config.refresh-interval-ms:3600000}")
    public void scheduledRefresh() {
        try {
            log.debug("Scheduled rune config refresh...");
            loadRuneConfig();
        } catch (Exception e) {
            log.warn("Scheduled config refresh failed: {}", e.getMessage());
        }
    }

    /**
     * Get rune configuration
     * Returns cached config or loads from Redis/config-service
     */
    public RuneConfig getRuneConfig() {
        if (cachedRuneConfig != null) {
            return cachedRuneConfig;
        }

        try {
            return loadRuneConfig();
        } catch (Exception e) {
            log.warn("Failed to load rune config: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Load rune config with Redis-first strategy
     */
    private RuneConfig loadRuneConfig() {
        String redisKey = REDIS_KEY_PREFIX + runeConfigPath.replace("/", ":");

        // 1. Try Redis first (if enabled)
        if (redisEnabled) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(redisKey);
                if (cachedJson != null && !cachedJson.isEmpty()) {
                    RuneConfig config = objectMapper.readValue(cachedJson, RuneConfig.class);
                    cachedRuneConfig = config;
                    log.debug("Loaded rune config from Redis cache");
                    return config;
                }
            } catch (Exception e) {
                log.warn("Redis lookup failed, falling back to config-service: {}", e.getMessage());
            }
        }

        // 2. Fallback to config-service
        try {
            ResponseEntity<byte[]> response = configServiceClient.getFile(runeConfigPath, lastETag);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String jsonContent = new String(response.getBody(), StandardCharsets.UTF_8);
                RuneConfig config = objectMapper.readValue(jsonContent, RuneConfig.class);

                // Cache in Redis
                if (redisEnabled) {
                    try {
                        redisTemplate.opsForValue().set(redisKey, jsonContent, redisTtlHours, TimeUnit.HOURS);
                        log.info("Cached rune config in Redis (TTL: {} hours)", redisTtlHours);
                    } catch (Exception e) {
                        log.warn("Failed to cache in Redis: {}", e.getMessage());
                    }
                }

                // Update ETag for next request
                if (response.getHeaders().getETag() != null) {
                    lastETag = response.getHeaders().getETag();
                }

                cachedRuneConfig = config;
                log.info("Loaded rune config from config-service: {} rune types",
                    config.getRuneList() != null ? config.getRuneList().size() : 0);
                return config;

            } else if (response.getStatusCode().value() == 304) {
                // Not modified, use cached config
                log.debug("Rune config not modified (304), using cached version");
                return cachedRuneConfig;
            }
        } catch (Exception e) {
            log.error("Failed to load rune config from config-service: {}", e.getMessage());
        }

        // 3. Return null if all strategies fail (caller will use defaults)
        log.warn("All config loading strategies failed, returning null");
        return null;
    }

    /**
     * Force reload config (bypass cache)
     */
    public void forceReload() {
        log.info("Force reloading rune config...");
        lastETag = null;
        cachedRuneConfig = null;

        if (redisEnabled) {
            String redisKey = REDIS_KEY_PREFIX + runeConfigPath.replace("/", ":");
            redisTemplate.delete(redisKey);
        }

        loadRuneConfig();
    }
}
