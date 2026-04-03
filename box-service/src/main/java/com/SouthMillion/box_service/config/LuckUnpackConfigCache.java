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

    public void ensureLoaded() {
        String path = props.getConfig().getKaixiangPath();

        // 1. Try Redis first (if enabled)
        if (redisEnabled) {
            String redisKey = toRedisKey(path);
            String cached = redis.opsForValue().get(redisKey);
            if (cached != null && !cached.isBlank()) {
                log.debug("[LuckUnpackConfigCache] Redis HIT path={}", path);
                try {
                    raw = om.readValue(cached, new TypeReference<>() {});
                    return;
                } catch (Exception e) {
                    log.warn("Failed to parse cached JSON from Redis, will reload: {}", e.toString());
                }
            }
            log.debug("[LuckUnpackConfigCache] Redis MISS path={}", path);
        }

        // 2. Call config-service with ETag
        String cur = etag.get();
        ResponseEntity<byte[]> resp = cfg.getFile(path, cur);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
            try {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                raw = om.readValue(json, new TypeReference<>() {});

                // 3. Cache in Redis
                if (redisEnabled) {
                    String redisKey = toRedisKey(path);
                    redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
                    log.debug("[LuckUnpackConfigCache] Cached in Redis path={}", path);
                }
            } catch (Exception ignore) {}
            if (resp.getHeaders().getETag()!=null) etag.set(resp.getHeaders().getETag());
        } else if (resp.getStatusCode().value() == 304) {
            return;
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

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }
}