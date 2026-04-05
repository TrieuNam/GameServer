package com.SouthMillion.webSocket_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.service.client.ConfigFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Lookup config JSON with Redis-first strategy.
 *
 * Key pattern:
 * - cfg:file:{path-with-colon}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigSnapshotLookupService {

    private final StringRedisTemplate redis;
    private final ConfigFeign configFeign;
    private final ObjectMapper objectMapper;

    @Value("${app.redis-preload.ttl-hours:24}")
    private long ttlHours;

    @Value("${app.redis-preload.allow-remote-fallback-on-miss:false}")
    private boolean allowRemoteFallbackOnMiss;

    public LookupResult getRaw(String path) {
        String key = toRedisFileKey(path);
        String cached = redis.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            touchRedisKey(key);
            return new LookupResult(path, "REDIS", cached, true);
        }

        if (!allowRemoteFallbackOnMiss) {
            throw new IllegalStateException("Config not preloaded in Redis path=" + path);
        }

        return loadFromConfigService(path, key);
    }

    public LookupResult warmFromRemote(String path) {
        return loadFromConfigService(path, toRedisFileKey(path));
    }

    private LookupResult cacheAndBuildResult(String path, String key, String source, String json, boolean cached) {
        redis.opsForValue().set(key, json, ttlHours, TimeUnit.HOURS);
        return new LookupResult(path, source, json, cached);
    }

    private LookupResult loadFromConfigService(String path, String key) {
        ResponseEntity<byte[]> resp = configFeign.getFile(path, null);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Cannot fetch config path=" + path + " status=" + resp.getStatusCode());
        }

        String json = new String(resp.getBody(), StandardCharsets.UTF_8);
        return cacheAndBuildResult(path, key, "CONFIG_SERVICE", json, false);
    }

    public JsonNode getJson(String path) {
        try {
            return objectMapper.readTree(getRaw(path).payload());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse config JSON path=" + path + ": " + e.getMessage(), e);
        }
    }

    public Map<String, Object> inspect(String path) {
        String key = toRedisFileKey(path);
        String cached = redis.opsForValue().get(key);
        Map<String, Object> out = new HashMap<>();
        out.put("path", path);
        out.put("source", (cached != null && !cached.isBlank()) ? "REDIS" : "MISS");
        out.put("cached", cached != null && !cached.isBlank());
        out.put("size", cached == null ? 0 : cached.length());
        out.put("redisKey", key);
        out.put("remoteFallbackOnMiss", allowRemoteFallbackOnMiss);
        return out;
    }

    public Map<String, Object> refreshFromRemote(String path) {
        LookupResult result = warmFromRemote(path);
        Map<String, Object> out = inspect(path);
        out.put("source", result.source());
        out.put("size", result.payload() == null ? 0 : result.payload().length());
        out.put("cached", true);
        return out;
    }

    private void touchRedisKey(String key) {
        if (key == null || key.isBlank() || ttlHours <= 0) {
            return;
        }
        try {
            redis.expire(key, ttlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[ConfigSnapshotLookupService] redis ttl touch failed key={} ex={}", key, e.toString());
        }
    }

    private String toRedisFileKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    public record LookupResult(String path, String source, String payload, boolean cached) {}
}
