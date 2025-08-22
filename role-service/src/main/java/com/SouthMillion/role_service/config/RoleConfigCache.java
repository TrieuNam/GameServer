package com.SouthMillion.role_service.config;

import com.SouthMillion.role_service.service.client.ConfigFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleConfigCache {

    private final ConfigFeign configFeign;
    private final ObjectMapper om = new ObjectMapper();

    private volatile String etagRoleExp;
    private volatile String etagRoleName;
    private volatile String etagOtherCfg;
    private volatile String etagKeyCfg;

    private final AtomicReference<ExpTable> expRef = new AtomicReference<>(new ExpTable());
    private final AtomicReference<List<String>> namePoolRef = new AtomicReference<>(List.of("Player"));
    private final AtomicReference<Defaults> defaultsRef = new AtomicReference<>(new Defaults());

    // ==== Public getters ====
    public long needExp(int level) {
        var e = expRef.get();
        return e.needByLevel.getOrDefault(level, Math.max(1, (long) level * 100L));
    }

    public int maxLevel() {
        return Math.max(1, expRef.get().maxLevel);
    }

    public List<String> namePool() {
        return namePoolRef.get();
    }

    public Defaults defaults() {
        return defaultsRef.get();
    }

    // ==== Refresh ====
    public void refreshAllIfNeeded() {
        refreshRoleExpIfNeeded();
        refreshRoleNameIfNeeded();
        refreshOtherOrKeyIfNeeded();
    }

    public void refreshRoleExpIfNeeded() {
        try {
            ResponseEntity<byte[]> resp = configFeign.getRoleExp(etagRoleExp);
            if (resp.getStatusCodeValue() == 304) return;
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return;
            etagRoleExp = stripQuotes(resp.getHeaders().getETag());

            Map<String, Object> m = om.readValue(resp.getBody(), new TypeReference<>() {
            });
            ExpTable parsed = parseRoleExp(m);
            if (parsed != null) expRef.set(parsed);
        } catch (Exception e) {
            log.warn("refreshRoleExpIfNeeded: {}", e.toString());
        }
    }

    public void refreshRoleNameIfNeeded() {
        try {
            ResponseEntity<byte[]> resp = configFeign.getRoleName(etagRoleName);
            if (resp.getStatusCodeValue() == 304) return;
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return;
            etagRoleName = stripQuotes(resp.getHeaders().getETag());

            Map<String, Object> m = om.readValue(resp.getBody(), new TypeReference<>() {
            });
            List<String> names = parseRoleNamePool(m);
            if (!names.isEmpty()) namePoolRef.set(names);
        } catch (Exception e) {
            log.info("refreshRoleNameIfNeeded: {}", e.getMessage());
        }
    }

    public void refreshOtherOrKeyIfNeeded() {
        // đọc default stat nếu có trong otherconfig/keyconfig
        try {
            ResponseEntity<byte[]> resp = configFeign.getOtherCfg(etagOtherCfg);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                etagOtherCfg = stripQuotes(resp.getHeaders().getETag());
                Map<String, Object> m = om.readValue(resp.getBody(), new TypeReference<>() {
                });
                Defaults d = parseDefaults(m);
                if (d != null) defaultsRef.set(d);
                return;
            } else if (resp.getStatusCodeValue() == 304) {
                return;
            }
        } catch (Exception ignore) {
        }

        try {
            ResponseEntity<byte[]> resp = configFeign.getKeyCfg(etagKeyCfg);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                etagKeyCfg = stripQuotes(resp.getHeaders().getETag());
                Map<String, Object> m = om.readValue(resp.getBody(), new TypeReference<>() {
                });
                Defaults d = parseDefaults(m);
                if (d != null) defaultsRef.set(d);
            }
        } catch (Exception e) {
            log.info("refreshKeyCfgIfNeeded: {}", e.getMessage());
        }
    }

    // ==== Parsing helpers ====
    private ExpTable parseRoleExp(Map<String, Object> body) {
        Map<Integer, Long> need = new HashMap<>();
        int max = asInt(firstNonNull(body.get("max"), body.get("maxLevel"), body.get("max_level")), 100);

        Object needObj = firstNonNull(body.get("need"), body.get("expNeed"), body.get("levelNeed"));
        if (needObj instanceof Map<?, ?> m) {
            for (var e : m.entrySet()) {
                Integer lv = asInt(e.getKey(), null);
                Long val = asLong(e.getValue(), null);
                if (lv != null && val != null && lv > 0) need.put(lv, val);
            }
        } else {
            Object levels = firstNonNull(body.get("levels"), body.get("table"), body.get("data"));
            if (levels instanceof List<?> arr) {
                for (Object o : arr) {
                    if (o instanceof Map<?, ?> r) {
                        Integer lv = asInt(firstNonNull(r.get("level"), r.get("lv")), null);
                        Long val = asLong(firstNonNull(r.get("need"), r.get("exp"), r.get("needExp")), null);
                        if (lv != null && val != null && lv > 0) need.put(lv, val);
                    }
                }
            } else {
                for (var e : body.entrySet()) {
                    Integer lv = asInt(e.getKey(), null);
                    Long val = asLong(e.getValue(), null);
                    if (lv != null && val != null && lv > 0) need.put(lv, val);
                }
            }
        }
        if (need.isEmpty()) for (int lv = 1; lv <= max; lv++) need.put(lv, lv * 100L);
        return new ExpTable(need, Math.max(1, max));
    }

    private List<String> parseRoleNamePool(Map<String, Object> body) {
        List<String> out = new ArrayList<>();
        pushAll(out, body.get("names"));
        pushAll(out, body.get("pool"));
        pushAll(out, body.get("list"));
        pushAll(out, body.get("prefix"));
        if (out.isEmpty()) out.add("Player");
        return out;
    }

    /**
     * Parse default stat nếu có (chịu nhiều layout)
     */
    private Defaults parseDefaults(Map<String, Object> body) {
        Map<String, Object> src = body;
        Object roleDefault = firstNonNull(body.get("role_default"), body.get("roleDefault"), body.get("defaults"));
        if (roleDefault instanceof Map<?, ?> m) src = (Map<String, Object>) m;

        long hp = asLong(firstNonNull(src.get("hp"), src.get("base_hp")), 100L);
        long atk = asLong(firstNonNull(src.get("attack"), src.get("atk"), src.get("attack_value")), 10L);
        long def = asLong(firstNonNull(src.get("defense"), src.get("def"), src.get("defense_value")), 5L);
        int spd = asInt(firstNonNull(src.get("speed"), src.get("spd")), 5);

        long hpPerLv = asLong(firstNonNull(src.get("hp_per_level"), src.get("hpPerLv")), 10L);
        long atkPerLv = asLong(firstNonNull(src.get("atk_per_level"), src.get("atkPerLv")), 2L);
        long defPerLv = asLong(firstNonNull(src.get("def_per_level"), src.get("defPerLv")), 1L);
        int spdPerLv = asInt(firstNonNull(src.get("spd_per_level"), src.get("spdPerLv")), 0);

        return new Defaults(hp, atk, def, spd, hpPerLv, atkPerLv, defPerLv, spdPerLv);
    }

    // Utils
    private static void pushAll(List<String> dst, Object obj) {
        if (obj instanceof List<?> arr) for (Object o : arr) if (o != null) dst.add(String.valueOf(o));
    }

    private static Object firstNonNull(Object... a) {
        for (Object o : a) if (o != null) return o;
        return null;
    }

    private static String stripQuotes(String etag) {
        if (etag == null) return null;
        String t = etag.trim();
        if (t.startsWith("W/\"") && t.endsWith("\"")) return t.substring(3, t.length() - 1);
        if (t.startsWith("\"") && t.endsWith("\"")) return t.substring(1, t.length() - 1);
        return t;
    }

    private static Integer asInt(Object o, Integer def) {
        try {
            if (o instanceof Number n) return n.intValue();
            if (o != null) return Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception ignore) {
        }
        return def;
    }

    private static Long asLong(Object o, Long def) {
        try {
            if (o instanceof Number n) return n.longValue();
            if (o != null) return Long.parseLong(String.valueOf(o).trim());
        } catch (Exception ignore) {
        }
        return def;
    }

    // holders
    private record ExpTable(Map<Integer, Long> needByLevel, int maxLevel) {
        private ExpTable() {
            this(new HashMap<>(), 100);
        }
    }

    @Getter
    public static class Defaults {
        private final long baseHp, baseAtk, baseDef;
        private final int baseSpd;
        private final long hpPerLv, atkPerLv, defPerLv;
        private final int spdPerLv;

        public Defaults() {
            this(100, 10, 5, 5, 10, 2, 1, 0);
        }

        public Defaults(long baseHp, long baseAtk, long baseDef, int baseSpd, long hpPerLv, long atkPerLv, long defPerLv, int spdPerLv) {
            this.baseHp = baseHp;
            this.baseAtk = baseAtk;
            this.baseDef = baseDef;
            this.baseSpd = baseSpd;
            this.hpPerLv = hpPerLv;
            this.atkPerLv = atkPerLv;
            this.defPerLv = defPerLv;
            this.spdPerLv = spdPerLv;
        }
    }
}