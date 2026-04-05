package com.SouthMillion.shop_service.config;

import com.SouthMillion.shop_service.service.config.ConfigFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Shop config cache with Redis-first lookup strategy.
 * 1. Check Redis cache first
 * 2. If miss, call config-service
 * 3. Cache result in Redis for future requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopConfigCache {

    private final ConfigFeign configFeign;
    private final ObjectMapper om;
    private final StringRedisTemplate redis;

    @Value("${app.config.cloth}")
    private String clothPath;
    @Value("${app.config.common}")
    private String commonPath;
    @Value("${app.config.shenmi}")
    private String shenmiPath;

    @Value("${shop.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${shop.config.redis-ttl-hours:24}")
    private long redisTtlHours;

    private final Cache<String, JsonNode> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(64)
            .build();

    private final Map<String, String> etags = new ConcurrentHashMap<>();

    public JsonNode cloth() { return getJson(clothPath); }
    public JsonNode common() { return getJson(commonPath); }
    public JsonNode shenmi() { return getJson(shenmiPath); }

    private JsonNode getJson(String path) {
        JsonNode localCached = cache.getIfPresent(path);

        // 1. Try Redis first (if enabled)
        if (redisEnabled) {
            String redisKey = toRedisKey(path);
            try {
                String cached = redis.opsForValue().get(redisKey);
                if (cached != null && !cached.isBlank()) {
                    log.debug("[ShopConfigCache] Redis HIT path={}", path);
                    JsonNode node = safeTree(cached);
                    cache.put(path, node);
                    touchRedisKey(redisKey);
                    return node;
                }
                log.debug("[ShopConfigCache] Redis MISS path={}", path);
            } catch (Exception e) {
                log.warn("[ShopConfigCache] Redis read failed path={} ex={}", path, e.toString());
                try {
                    redis.delete(redisKey);
                } catch (Exception ignored) {
                    // ignore corrupt-cache cleanup failure
                }
            }
        }

        // 2. Try local Caffeine cache + config-service fallback
        String etag = etags.get(path);
        try {
            ResponseEntity<byte[]> res = configFeign.getFile(path, etag);
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                String body = new String(res.getBody(), StandardCharsets.UTF_8);
                JsonNode node = safeTree(body);
                cache.put(path, node);
                String newTag = res.getHeaders().getETag();
                if (newTag != null) etags.put(path, newTag);

                if (redisEnabled) {
                    String redisKey = toRedisKey(path);
                    redis.opsForValue().set(redisKey, body, redisTtlHours, TimeUnit.HOURS);
                    log.debug("[ShopConfigCache] Cached in Redis path={}", path);
                }
                return node;
            }

            if (res.getStatusCode().value() == 304 && localCached != null) {
                return localCached;
            }
        } catch (Exception e) {
            log.warn("[ShopConfigCache] config fetch failed path={} ex={}", path, e.toString());
        }

        if (localCached != null) {
            return localCached;
        }

        // fallback: nếu lần đầu cache rỗng thì gọi lại không ETag
        try {
            ResponseEntity<byte[]> res2 = configFeign.getFile(path, null);
            if (!res2.getStatusCode().is2xxSuccessful() || res2.getBody() == null) {
                throw new RuntimeException("Cannot load config path=" + path + " status=" + res2.getStatusCode());
            }
            String body = new String(res2.getBody(), StandardCharsets.UTF_8);
            JsonNode node = safeTree(body);
            cache.put(path, node);
            String newTag = res2.getHeaders().getETag();
            if (newTag != null) etags.put(path, newTag);

            if (redisEnabled) {
                String redisKey = toRedisKey(path);
                redis.opsForValue().set(redisKey, body, redisTtlHours, TimeUnit.HOURS);
            }
            return node;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load config path=" + path, e);
        }
    }

    private JsonNode safeTree(String json) {
        try { return om.readTree(json); }
        catch (Exception e) { throw new RuntimeException("Bad config JSON", e); }
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[ShopConfigCache] redis ttl touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }
}