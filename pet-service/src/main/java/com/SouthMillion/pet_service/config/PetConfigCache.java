package com.SouthMillion.pet_service.config;

import com.SouthMillion.pet_service.service.client.ConfigFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetConfigCache {

    private final ConfigFeign cfg;
    private final AppProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private final AtomicReference<String> etagPetAuto = new AtomicReference<>();
    @Getter
    private volatile Map<String, Object> petAuto = Map.of();

    private static final long REFRESH_INTERVAL_MS = 30_000;
    private volatile long lastCheckMs = 0L;
    private final AtomicBoolean loading = new AtomicBoolean(false);

    public void forceReload() { loadPetAuto(/*force*/1, /*respectTtl*/false); }
    public void ensureLoaded(){ loadPetAuto(/*force*/0, /*respectTtl*/true); }

    @SuppressWarnings("unchecked")
    public Map<String,Object> other() {
        ensureLoaded();
        var list = (List<Map<String,Object>>) petAuto.getOrDefault("other", List.of());
        return list.isEmpty()? Map.of() : list.get(0);
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Map<String,Object>> petBaseById() {
        ensureLoaded();
        var list = (List<Map<String,Object>>) petAuto.getOrDefault("pet", List.of());
        Map<Integer, Map<String,Object>> m = new HashMap<>();
        for (var row : list) {
            Integer id = asInt(row.get("pet_id"));
            if (id != null) m.put(id, row);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, List<Map<String,Object>>> petUpByType() {
        ensureLoaded();
        var list = (List<Map<String,Object>>) petAuto.getOrDefault("pet_up", List.of());
        Map<Integer, List<Map<String,Object>>> m = new HashMap<>();
        for (var r : list) {
            Integer t = asInt(r.get("pet_type"));
            m.computeIfAbsent(t, k -> new ArrayList<>()).add(r);
        }
        // sort by pet_level asc
        for (var e : m.values()) {
            e.sort(Comparator.comparingInt(o -> asInt(o.get("pet_level"))));
        }
        return m;
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
        return null;
    }

    private void loadPetAuto(int force, boolean respectTtl) {
        final long now = System.currentTimeMillis();
        if (respectTtl && (now - lastCheckMs) < REFRESH_INTERVAL_MS) return;
        if (!loading.compareAndSet(false, true)) return;
        try {
            lastCheckMs = now;
            String rel = props.getPet_auto();
            String et = (force == 1 ? null : etagPetAuto.get());
            ResponseEntity<byte[]> resp;
            try {
                resp = cfg.byPath(rel, et, force);
            } catch (FeignException.NotFound nf) {
                return;
            } catch (FeignException.NotImplemented nm) {
                return;
            }
            if (resp == null || resp.getStatusCode().is4xxClientError() || resp.getStatusCode().is5xxServerError()) return;

            String newEtag = resp.getHeaders().getETag();
            byte[] body = resp.getBody();
            if (body == null || body.length == 0) return;

            Map<String,Object> parsed = om.readValue(new String(body, StandardCharsets.UTF_8), new TypeReference<>() {});
            this.petAuto = parsed != null ? parsed : Map.of();
            if (newEtag != null && !newEtag.isBlank()) etagPetAuto.set(newEtag);
        } catch (Exception e) {
            log.warn("load pet_auto error: {}", e.toString());
        } finally {
            loading.set(false);
        }
    }
}