package com.SouthMillion.box_service.config;

import com.SouthMillion.box_service.service.client.ConfigFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Luck unpack config cache with Redis-first lookup strategy.
 * 1. Check Redis cache first
 * 2. If miss, call config-service
 * 3. Cache result in Redis for future requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LuckUnpackConfigCache {
    private final ConfigFeign cfg;
    private final AppProperties props;
    private final ObjectMapper om = new ObjectMapper();
    private final StringRedisTemplate redis;

    private final AtomicReference<String> etag = new AtomicReference<>();
    @Getter private volatile Map<String,Object> raw = Map.of();

    @Value("${box.luck.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${box.luck.redis-ttl-hours:24}")
    private long redisTtlHours;
    @Value("${box.luck.allow-remote-fallback-on-miss:false}")
    private boolean allowRemoteFallbackOnMiss;

    public void ensureLoaded() {
        String path = props.getConfig().getKaixiangPath();
        if (path == null || path.isBlank()) {
            return;
        }

        String redisKey = toRedisKey(path);

        // 1. Try Redis first (cache-aside read path)
        if (redisEnabled) {
            try {
                String cached = redis.opsForValue().get(redisKey);
                if (cached != null && !cached.isBlank()) {
                    log.debug("[LuckUnpackConfigCache] Redis HIT path={}", path);
                    raw = om.readValue(cached, new TypeReference<>() {});
                    touchRedisKey(redisKey);
                    return;
                }
                log.debug("[LuckUnpackConfigCache] Redis MISS path={}", path);
            } catch (Exception e) {
                log.warn("Failed to read cached JSON from Redis, will reload: {}", e.toString());
                try {
                    redis.delete(redisKey);
                } catch (Exception ignored) {
                    // ignore corrupt-cache cleanup failure
                }
            }
        }

        if (!allowRemoteFallbackOnMiss) {
            if (!raw.isEmpty()) {
                log.warn("[LuckUnpackConfigCache] Redis miss for path={} but remote fallback is disabled; keep last in-memory snapshot", path);
                return;
            }
            throw new IllegalStateException("kaixiangdaji.json missing from Redis while box.luck.allow-remote-fallback-on-miss=false");
        }

        // 2. Cache miss -> call config-service and repopulate Redis
        try {
            String cur = etag.get();
            ResponseEntity<byte[]> resp = cfg.getFile(path, cur);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                raw = om.readValue(json, new TypeReference<>() {});
                if (resp.getHeaders().getETag() != null) {
                    etag.set(resp.getHeaders().getETag());
                }

                if (redisEnabled) {
                    redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
                    log.debug("[LuckUnpackConfigCache] Cached in Redis path={}", path);
                }
            }
        } catch (feign.FeignException ex) {
            if (ex.status() != 304) {
                log.warn("[LuckUnpackConfigCache] config fetch failed path={} status={} ex={}",
                        path, ex.status(), ex.getMessage());
            }
        } catch (Exception e) {
            log.warn("[LuckUnpackConfigCache] unexpected reload failure path={} ex={}", path, e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> reward() {
        ensureLoaded();
        return (List<Map<String,Object>>) raw.getOrDefault("reward", List.of());
    }
    @SuppressWarnings("unchecked")
    public List<Map<String,String>> other() {
        ensureLoaded();
        return (List<Map<String,String>>) raw.getOrDefault("other", List.of());
    }

    public void clear() {
        String path = props.getConfig().getKaixiangPath();
        if (redisEnabled && path != null && !path.isBlank()) {
            try {
                redis.delete(toRedisKey(path));
            } catch (Exception e) {
                log.debug("[LuckUnpackConfigCache] redis clear failed path={} ex={}", path, e.toString());
            }
        }
        etag.set(null);
        raw = Map.of();
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[LuckUnpackConfigCache] redis ttl touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }
}