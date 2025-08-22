package com.SouthMillion.shop_service.config;

import com.SouthMillion.shop_service.service.config.ConfigFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ShopConfigCache {

    private final ConfigFeign configFeign;
    private final ObjectMapper om;

    @Value("${app.config.cloth}")
    private String clothPath;
    @Value("${app.config.common}")
    private String commonPath;
    @Value("${app.config.shenmi}")
    private String shenmiPath;

    private final Cache<String, JsonNode> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(64)
            .build();

    private final Map<String, String> etags = new ConcurrentHashMap<>();

    public JsonNode cloth() { return getJson(clothPath); }
    public JsonNode common() { return getJson(commonPath); }
    public JsonNode shenmi() { return getJson(shenmiPath); }

    private JsonNode getJson(String path) {
        JsonNode cached = cache.getIfPresent(path);
        String etag = etags.get(path);

        ResponseEntity<byte[]> res = configFeign.byPath(path, etag);
        if (res.getStatusCode().is2xxSuccessful() && res.getBody()!=null) {
            String body = new String(res.getBody(), StandardCharsets.UTF_8);
            JsonNode node = safeTree(body);
            cache.put(path, node);
            String newTag = res.getHeaders().getETag();
            if (newTag != null) etags.put(path, newTag);
            return node;
        }
        // 304 Not Modified
        if (cached != null) return cached;

        // fallback: nếu 304 nhưng cache null (lần đầu), gọi lại không header
        ResponseEntity<byte[]> res2 = configFeign.byPath(path, null);
        String body = new String(res2.getBody(), StandardCharsets.UTF_8);
        JsonNode node = safeTree(body);
        cache.put(path, node);
        String newTag = res2.getHeaders().getETag();
        if (newTag != null) etags.put(path, newTag);
        return node;
    }

    private JsonNode safeTree(String json) {
        try { return om.readTree(json); }
        catch (Exception e) { throw new RuntimeException("Bad config JSON", e); }
    }
}