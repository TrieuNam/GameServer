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
public class LuckUnpackConfigCache {
    private final ConfigFeign cfg;
    private final AppProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private final AtomicReference<String> etag = new AtomicReference<>();
    @Getter private volatile Map<String,Object> raw = Map.of();

    public void ensureLoaded() {
        String cur = etag.get();
        ResponseEntity<byte[]> resp = cfg.getFile(props.getConfig().getKaixiangPath(), cur);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
            try {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                raw = om.readValue(json, new TypeReference<>() {});
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
}