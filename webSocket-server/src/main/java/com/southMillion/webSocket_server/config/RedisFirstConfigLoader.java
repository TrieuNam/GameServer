package com.SouthMillion.webSocket_server.config;

import com.SouthMillion.webSocket_server.service.client.ConfigFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Redis-first config loader utility.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Check Redis first (fast, < 1ms)</li>
 *   <li>If miss, call config-service</li>
 *   <li>Cache result in Redis for 24h</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 * public class MyConfigCache {
 *     private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyConfigCache.class);
 *     private final RedisFirstConfigLoader loader;
 *
 *     public void refresh() {
 *         String json = loader.loadConfig("gameworld/item/equipment.json", currentETag);
 *         // parse json...
 *     }
 * }
 * </pre>
 *
 * <p>Benefits:
 * <ul>
 *   <li>Reduces config-service load during login peaks</li>
 *   <li>Faster config access (Redis < 1ms vs HTTP 10-50ms)</li>
 *   <li>Shared cache across all services</li>
 * </ul>
 */
@Slf4j
public class RedisFirstConfigLoader {

    private final StringRedisTemplate redis;
    private final ConfigFeign configFeign;
    private final ObjectMapper objectMapper;
    private final long ttlHours;

    public RedisFirstConfigLoader(StringRedisTemplate redis, ConfigFeign configFeign,
                                   ObjectMapper objectMapper, long ttlHours) {
        this.redis = redis;
        this.configFeign = configFeign;
        this.objectMapper = objectMapper;
        this.ttlHours = ttlHours;
    }

    /**
     * Load config with Redis-first strategy.
     *
     * @param path Config file path (e.g., "gameworld/item/equipment.json")
     * @param currentETag Current ETag for HTTP 304 check (can be null)
     * @return Config JSON string, or null if not found
     */
    public String loadConfig(String path, String currentETag) {
        String redisKey = toRedisKey(path);

        // 1. Try Redis first
        String cached = redis.opsForValue().get(redisKey);
        if (cached != null && !cached.isBlank()) {
            log.debug("[RedisFirstConfig] HIT path={}", path);
            touch(redisKey);
            touch(toETagKey(path));
            return cached;
        }

        // 2. Redis miss → call config-service
        log.debug("[RedisFirstConfig] MISS path={}, calling config-service", path);
        try {
            ResponseEntity<byte[]> resp = configFeign.getFile(path, currentETag);

            // 304 Not Modified → use current cached data
            if (resp.getStatusCode().value() == 304) {
                log.debug("[RedisFirstConfig] 304 Not Modified path={}", path);
                return null; // caller should keep existing data
            }

            // 200 OK → cache in Redis
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                redis.opsForValue().set(redisKey, json, ttlHours, TimeUnit.HOURS);

                // Also cache metadata
                String etag = resp.getHeaders().getETag();
                if (etag != null) {
                    redis.opsForValue().set(toETagKey(path), etag, ttlHours, TimeUnit.HOURS);
                }

                log.info("[RedisFirstConfig] Loaded and cached path={} size={}bytes", path, json.length());
                return json;
            }
        } catch (FeignException ex) {
            if (ex.status() == 304) {
                return null; // 304 → keep existing
            }
            log.warn("[RedisFirstConfig] Failed to load path={}: {}", path, ex.getMessage());
        } catch (Exception ex) {
            log.warn("[RedisFirstConfig] Failed to load path={}: {}", path, ex.getMessage());
        }

        return null;
    }

    /**
     * Get cached ETag from Redis.
     */
    public String getCachedETag(String path) {
        return redis.opsForValue().get(toETagKey(path));
    }

    /**
     * Clear cached config (for testing or manual refresh).
     */
    public void clearCache(String path) {
        redis.delete(toRedisKey(path));
        redis.delete(toETagKey(path));
        log.info("[RedisFirstConfig] Cleared cache for path={}", path);
    }

    private void touch(String key) {
        if (key == null || key.isBlank() || ttlHours <= 0) {
            return;
        }
        try {
            redis.expire(key, ttlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[RedisFirstConfig] ttl touch failed key={} ex={}", key, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    private String toETagKey(String path) {
        return "cfg:file:meta:etag:" + path.replace('/', ':');
    }
}
