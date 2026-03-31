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

    public LookupResult getRaw(String path) {
        String key = toRedisFileKey(path);
        String cached = redis.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            return new LookupResult(path, "REDIS", cached, true);
        }

        ResponseEntity<byte[]> resp = configFeign.getFile(path, null);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Cannot fetch config path=" + path + " status=" + resp.getStatusCode());
        }

        String json = new String(resp.getBody(), StandardCharsets.UTF_8);
        redis.opsForValue().set(key, json, ttlHours, TimeUnit.HOURS);
        return new LookupResult(path, "CONFIG_SERVICE", json, false);
    }

    public JsonNode getJson(String path) {
        try {
            return objectMapper.readTree(getRaw(path).payload());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse config JSON path=" + path + ": " + e.getMessage(), e);
        }
    }

    public Map<String, Object> inspect(String path) {
        LookupResult result = getRaw(path);
        Map<String, Object> out = new HashMap<>();
        out.put("path", result.path());
        out.put("source", result.source());
        out.put("cached", result.cached());
        out.put("size", result.payload() == null ? 0 : result.payload().length());
        out.put("redisKey", toRedisFileKey(path));
        return out;
    }

    private String toRedisFileKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    public record LookupResult(String path, String source, String payload, boolean cached) {}
}
