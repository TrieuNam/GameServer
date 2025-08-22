package com.southMillion.equip_service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.southMillion.equip_service.service.client.ConfigFeign;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class EquipmentConfigCache {

    private final ConfigFeign cfg;
    private final ObjectMapper om = new ObjectMapper();

    private final AtomicReference<String> etag = new AtomicReference<>(null);
    private final AtomicReference<Map<Integer, EquipRow>> byId = new AtomicReference<>(Map.of());

    @Getter
    public static class EquipRow {
        public int id;
        public int part;            // slot type 0..11
        public int hp_max;
        public int att_max;
        public int def_max;
        public int speed_max;
        public Integer frist_att;   // nếu cần map sang attr_type1
        public Integer second_att;  // nếu cần map sang attr_type2
    }

    public void ensureLoaded() {
        String cur = etag.get();
        ResponseEntity<byte[]> resp = cfg.getItem("equipment", cur);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
            try {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                // file có dạng: { "wuqi":[{row}...], "toukui":[{row}...], ... }
                Map<String, List<Map<String,String>>> root = om.readValue(json, new TypeReference<>() {});
                Map<Integer, EquipRow> tmp = new HashMap<>();
                root.values().forEach(arr -> {
                    for (var m : arr) {
                        try {
                            EquipRow r = new EquipRow();
                            r.id = parseInt(m.get("id"));
                            r.part = parseInt(m.get("part"));
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
            } catch (Exception ignore) {}
            if (resp.getHeaders().getETag()!=null) etag.set(resp.getHeaders().getETag());
        }
    }

    public Optional<EquipRow> find(int itemId) {
        ensureLoaded();
        return Optional.ofNullable(byId.get().get(itemId));
    }

    private static int parseInt(String s){ try { return Integer.parseInt(s); } catch (Exception e){ return 0; } }
    private static Integer parseNullableInt(String s){ try { return s==null?null:Integer.parseInt(s); } catch (Exception e){ return null; } }
}