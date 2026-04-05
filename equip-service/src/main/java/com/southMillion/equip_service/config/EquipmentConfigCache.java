package com.SouthMillion.equip_service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.equip_service.service.client.ConfigFeign;
import feign.FeignException;
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
 * EquipmentConfigCache - Redis-first equipment config loader
 *
 * <p>Loads equipment templates from equipment.json with Redis-first strategy:
 * <ol>
 *   <li>Check Redis first (preloaded by websocket-server)</li>
 *   <li>If miss, call config-service</li>
 *   <li>Cache result in Redis for 24h</li>
 * </ol>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EquipmentConfigCache {

    private final ConfigFeign cfg;
    private final StringRedisTemplate redis;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${equip.config.redis-enabled:true}")
    private boolean redisEnabled;

    @Value("${equip.config.redis-ttl-hours:24}")
    private long redisTtlHours;

    private final AtomicReference<String> etag = new AtomicReference<>(null);
    private final AtomicReference<Map<Integer, EquipRow>> byId = new AtomicReference<>(Map.of());
    private final AtomicReference<String> unpackEtag = new AtomicReference<>(null);
    private final AtomicReference<Map<Integer, ColorAttrBonus>> colorAttrByGroup = new AtomicReference<>(Map.of());

    @Value("${equip.config.equipment-path:gameworld/item/equipment.json}")
    private String equipmentPath;

    @Value("${equip.config.unpack-path:gameworld/logicconfig/unpack.json}")
    private String unpackPath;

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

    @Getter
    public static class ColorAttrBonus {
        private final int attGroup;
        private final int attrType;
        private final int attrValue;

        public ColorAttrBonus(int attGroup, int attrType, int attrValue) {
            this.attGroup = attGroup;
            this.attrType = attrType;
            this.attrValue = attrValue;
        }
    }

    public void ensureLoaded() {
        String cur = etag.get();
        String json = null;

        try {
            String redisKey = toRedisKey(equipmentPath);

            // 1. Try Redis first (if enabled)
            if (redisEnabled) {
                json = redis.opsForValue().get(redisKey);
                if (json != null && !json.isBlank()) {
                    log.debug("[EquipmentConfigCache] Redis HIT path={}", equipmentPath);
                    try {
                        parseAndCache(json);
                        touchRedisKey(redisKey);
                        return;
                    } catch (Exception e) {
                        log.warn("[EquipmentConfigCache] Failed to parse cached JSON, reloading path={} ex={}",
                                equipmentPath, e.toString());
                        try {
                            redis.delete(redisKey);
                        } catch (Exception ignored) {
                            // ignore corrupt-cache cleanup failure
                        }
                    }
                }
                log.debug("[EquipmentConfigCache] Redis MISS path={}, calling config-service", equipmentPath);
            }

            // 2. Redis miss or disabled → call config-service
            ResponseEntity<byte[]> resp = cfg.getFile(equipmentPath, cur);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
                json = new String(resp.getBody(), StandardCharsets.UTF_8);
                parseAndCache(json);

                if (resp.getHeaders().getETag()!=null) etag.set(resp.getHeaders().getETag());

                // 3. Cache in Redis for next time
                if (redisEnabled && json != null) {
                    redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
                    log.info("[EquipmentConfigCache] Loaded and cached {} equipment templates", byId.get().size());
                }
            }
        } catch (FeignException ex) {
            if (ex.status() == 304) {
                return; // Not Modified - use existing cache
            }
            throw ex;
        }
    }

    private void parseAndCache(String json) {
        try {
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
    }

    public Optional<EquipRow> find(int itemId) {
        ensureLoaded();
        return Optional.ofNullable(byId.get().get(itemId));
    }

    public Optional<ColorAttrBonus> resolveColorAttr(Integer attGroup) {
        if (attGroup == null || attGroup <= 0) {
            return Optional.empty();
        }
        ensureColorAttrLoaded();
        return Optional.ofNullable(colorAttrByGroup.get().get(attGroup));
    }

    public Collection<EquipRow> allRows() {
        ensureLoaded();
        return byId.get().values();
    }

    private void ensureColorAttrLoaded() {
        if (!colorAttrByGroup.get().isEmpty()) {
            return;
        }

        String cur = unpackEtag.get();
        String json = null;
        try {
            String redisKey = toRedisKey(unpackPath);

            if (redisEnabled) {
                json = redis.opsForValue().get(redisKey);
                if (json != null && !json.isBlank()) {
                    log.debug("[EquipmentConfigCache] Redis HIT unpack path={}", unpackPath);
                    try {
                        parseColorAttrAndCache(json);
                        touchRedisKey(redisKey);
                        return;
                    } catch (Exception e) {
                        log.warn("[EquipmentConfigCache] Failed to parse cached unpack config, reloading path={} ex={}",
                                unpackPath, e.toString());
                        try {
                            redis.delete(redisKey);
                        } catch (Exception ignored) {
                            // ignore corrupt-cache cleanup failure
                        }
                    }
                }
            }

            ResponseEntity<byte[]> resp = cfg.getFile(unpackPath, cur);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                json = new String(resp.getBody(), StandardCharsets.UTF_8);
                parseColorAttrAndCache(json);

                if (resp.getHeaders().getETag() != null) {
                    unpackEtag.set(resp.getHeaders().getETag());
                }
                if (redisEnabled && json != null) {
                    redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
                }
            }
        } catch (FeignException ex) {
            if (ex.status() != 304) {
                log.warn("[EquipmentConfigCache] load unpack config failed path={} status={} ex={}",
                        unpackPath, ex.status(), ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseColorAttrAndCache(String json) {
        try {
            Map<String, Object> root = om.readValue(json, new TypeReference<>() {});
            Object rawColorAtt = root.get("color_att");
            List<?> rows = rawColorAtt instanceof List<?> list ? list : List.of();
            Map<Integer, ColorAttrBonus> tmp = new HashMap<>();
            for (Object entry : rows) {
                if (!(entry instanceof Map<?, ?> mapEntry)) {
                    continue;
                }
                Map<String, Object> row = (Map<String, Object>) mapEntry;
                int group = parseInt(row.get("att_group"));
                int attrType = parseInt(row.get("att_type"));
                int attrValue = parseInt(firstNonNull(row.get("att_num_max"), row.get("att_num"), row.get("num")));
                if (group > 0 && attrType > 0) {
                    // Mirror the client: keep the last row encountered for each att_group.
                    tmp.put(group, new ColorAttrBonus(group, attrType, attrValue));
                }
            }
            colorAttrByGroup.set(Collections.unmodifiableMap(tmp));
        } catch (Exception ex) {
            log.warn("[EquipmentConfigCache] parse unpack color_att failed: {}", ex.getMessage());
        }
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

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void touchRedisKey(String redisKey) {
        if (!redisEnabled || redisKey == null || redisKey.isBlank() || redisTtlHours <= 0) {
            return;
        }
        try {
            redis.expire(redisKey, redisTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("[EquipmentConfigCache] Redis TTL touch failed key={} ex={}", redisKey, e.toString());
        }
    }

    /**
     * Convert config path to Redis key format.
     * Example: "gameworld/item/equipment.json" → "cfg:file:gameworld:item:equipment.json"
     */
    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }
}