package com.SouthMillion.box_service.config;

import com.SouthMillion.box_service.service.client.ConfigFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class UnpackConfigCache {
    private final ConfigFeign cfg;
    private final AppProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private final AtomicReference<String> etag = new AtomicReference<>();
    @Getter private volatile Map<String,Object> raw = Map.of();

    public void ensureLoaded() {
        String cur = etag.get();
        ResponseEntity<byte[]> resp = cfg.byPath(props.getConfig().getUnpackPath(), cur);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
            try {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                raw = om.readValue(json, new TypeReference<>() {});
            } catch (Exception ignore) {}
            if (resp.getHeaders().getETag()!=null) etag.set(resp.getHeaders().getETag());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String,String>> randomLevel() {
        ensureLoaded();
        return (List<Map<String,String>>) raw.getOrDefault("random_level", List.of());
    }
    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> randomColor() {
        ensureLoaded();
        return (List<Map<String,Object>>) raw.getOrDefault("random_color", List.of());
    }
    @SuppressWarnings("unchecked")
    public List<Map<String,String>> other() {
        ensureLoaded();
        return (List<Map<String,String>>) raw.getOrDefault("other", List.of());
    }
    @SuppressWarnings("unchecked")
    public List<Map<String,String>> colorAtt() {
        ensureLoaded();
        return (List<Map<String,String>>) raw.getOrDefault("color_att", List.of());
    }
    @SuppressWarnings("unchecked")
    public List<Map<String,String>> fixedReward() {
        ensureLoaded();
        return (List<Map<String,String>>) raw.getOrDefault("fixed_reward", List.of());
    }
    @SuppressWarnings("unchecked")
    public List<Map<String,String>> shizhuangRate() {
        ensureLoaded();
        return (List<Map<String,String>>) raw.getOrDefault("shizhuang_rate", List.of());
    }
}