package com.SouthMillion.equip_service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.equip_service.service.client.ConfigFeign;
import feign.FeignException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@RequiredArgsConstructor
public class EquipmentConfigCache {

    private final ConfigFeign cfg;
    private final ObjectMapper om = new ObjectMapper();

    private final AtomicReference<String> etag = new AtomicReference<>(null);
    private final AtomicReference<Map<Integer, EquipRow>> byId = new AtomicReference<>(Map.of());

    @Value("${equip.config.equipment-path:gameworld/item/equipment.json}")
    private String equipmentPath;

    @Getter
    public static class EquipRow {
        public int id;
        public int part;            // slot type 0..11
        public Integer quality;     // optional in some configs
        public Integer level;       // optional in some configs
        public int hp_max;
        public int att_max;
        public int def_max;
        public int speed_max;
        public Integer frist_att;   // nếu cần map sang attr_type1
        public Integer second_att;  // nếu cần map sang attr_type2
    }

    public void ensureLoaded() {
        String cur = etag.get();
        try {
            ResponseEntity<byte[]> resp = cfg.getFile(equipmentPath, cur);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
                try {
                    String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                    // file có dạng: { "wuqi":[{row}...], "toukui":[{row}...], ... }
                    Map<String, List<Map<String, Object>>> root = om.readValue(json, new TypeReference<>() {});
                    Map<Integer, EquipRow> tmp = new HashMap<>();
                    root.values().forEach(arr -> {
                        for (var m : arr) {
                            try {
                                EquipRow r = new EquipRow();
                                r.id = parseInt(m.get("id"));
                                r.part = parseInt(m.get("part"));
                                r.quality = firstNullableInt(m, "quality", "color", "q");
                                r.level = firstNullableInt(m, "level", "lv");
                                r.hp_max = parseInt(m.get("hp_max"));
                                r.att_max = parseInt(m.get("att_max"));
                                r.def_max = parseInt(m.get("def_max"));
                                r.speed_max = parseInt(m.get("speed_max"));
                                r.frist_att = parseNullableInt(m.get("frist_att"));
                                r.second_att = parseNullableInt(m.get("second_att"));
                                if (r.id > 0) tmp.put(r.id, r);
                            } catch (Exception ignore) {}
                        }
                    });
                    byId.set(Collections.unmodifiableMap(tmp));
                } catch (Exception ex) {
                    log.warn("[EquipmentConfigCache] parse equipment config failed: {}", ex.getMessage());
                }
                if (resp.getHeaders().getETag()!=null) etag.set(resp.getHeaders().getETag());
            }
        } catch (FeignException ex) {
            if (ex.status() == 304) {
                return;
            }
            throw ex;
        }
    }

    public Optional<EquipRow> find(int itemId) {
        ensureLoaded();
        return Optional.ofNullable(byId.get().get(itemId));
    }

    public Collection<EquipRow> allRows() {
        ensureLoaded();
        return byId.get().values();
    }

    private static int parseInt(Object v){
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e){ return 0; }
    }

    private static Integer parseNullableInt(Object v){
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e){ return null; }
    }

    private static Integer firstNullableInt(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Integer v = parseNullableInt(m.get(k));
            if (v != null) return v;
        }
        return null;
    }
}