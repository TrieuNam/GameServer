package com.SouthMillion.box_service.config;

import com.SouthMillion.box_service.service.client.ConfigFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


@Component
@RequiredArgsConstructor
public class EquipmentIndex {

    private final UnpackConfigCache unpackCfg; // optional (để dành nếu bạn muốn dùng sau)
    private final com.SouthMillion.box_service.service.client.ConfigFeign configFeign;

    private final AtomicReference<String> etag = new AtomicReference<>(null);

    // Chỉ mục ưu tiên "bản thường" nếu trùng (is_special=0), nếu không có thì lấy bất kỳ
    private final AtomicReference<Map<Integer, Map<Integer, Map<Integer, Integer>>>> idxPref = new AtomicReference<>(null);

    // Chỉ mục đầy đủ (để bạn tự chọn among specials)
    private final AtomicReference<Map<Integer, Map<Integer, Map<Integer, List<Integer>>>>> idxAll = new AtomicReference<>(null);

    // Toàn bộ id
    private final AtomicReference<Set<Integer>> allEquipIds = new AtomicReference<>(null);

    // Row đã parse theo kiểu (để lấy stats nhanh)
    private final AtomicReference<Map<Integer, BoxDTOs.EquipRow>> metaById = new AtomicReference<>(null);

    private final ObjectMapper om = new ObjectMapper();

    @Getter(lazy = true)
    private final Random rng = new Random();

    private static int pInt(String s, int def) {
        try { return (s == null || s.isEmpty()) ? def : Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
    private static long pLong(String s, long def) {
        try { return (s == null || s.isEmpty()) ? def : Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private BoxDTOs.EquipRow parseRow(Map<String, String> m) {
        return new BoxDTOs.EquipRow(
                pInt(m.get("id"), 0),
                pInt(m.get("part"), -1),
                pInt(m.get("level"), -1),
                pInt(m.get("quality"), -1),
                pLong(m.get("hp_min"), 0), pLong(m.get("hp_max"), 0),
                pLong(m.get("att_min"), 0), pLong(m.get("att_max"), 0),
                pLong(m.get("def_min"), 0), pLong(m.get("def_max"), 0),
                pLong(m.get("speed_min"), 0), pLong(m.get("speed_max"), 0),
                pLong(m.get("exp"), 0), pLong(m.get("frist_att"), 0), pLong(m.get("second_att"), 0),
                pInt(m.get("item_type"), 0), pLong(m.get("sellprice"), 0), pInt(m.get("pile_limit"), 0),
                pInt(m.get("isdroprecord"), 0), pInt(m.get("invalid_time"), 0), pInt(m.get("is_special"), 0)
        );
    }

    @SuppressWarnings("unchecked")
    private void loadIfNeeded() {
        if (idxPref.get() != null && idxAll.get() != null && allEquipIds.get() != null && metaById.get() != null) return;

        String ifNoneMatch = etag.get();
        ResponseEntity<byte[]> resp = configFeign.getItem("equipment.json", ifNoneMatch);

        if (resp.getStatusCode().is2xxSuccessful()) {
            try {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                Map<String, List<Map<String, String>>> root = om.readValue(json, new TypeReference<>() {});

                Map<Integer, Map<Integer, Map<Integer, Integer>>> pref = new HashMap<>();
                Map<Integer, Map<Integer, Map<Integer, List<Integer>>>> all = new HashMap<>();
                Map<Integer, BoxDTOs.EquipRow> byId = new HashMap<>();
                Set<Integer> ids = new HashSet<>();

                root.values().forEach(arr -> {
                    for (var m : arr) {
                        BoxDTOs.EquipRow r = parseRow(m);
                        if (r.getId() <= 0 || r.getPart() < 0 || r.getLevel() <= 0 || r.getQuality() <= 0) continue;

                        // idxAll: list ids cho (p,q,l)
                        all.computeIfAbsent(r.getPart(), _k -> new HashMap<>())
                                .computeIfAbsent(r.getQuality(), _k -> new HashMap<>())
                                .computeIfAbsent(r.getLevel(), _k -> new ArrayList<>())
                                .add(r.getId());

                        // idxPref: nếu có trùng (p,q,l) → ưu tiên is_special=0
                        Map<Integer, Map<Integer, Integer>> byPart = pref.computeIfAbsent(r.getPart(), _k -> new HashMap<>());
                        Map<Integer, Integer> byQ = byPart.computeIfAbsent(r.getQuality(), _k -> new HashMap<>());
                        Integer existing = byQ.get(r.getLevel());
                        if (existing == null) {
                            byQ.put(r.getLevel(), r.getId());
                        } else {
                            // Nếu existing trùng là special mà bản mới là thường → thay thế
                            BoxDTOs.EquipRow existRow = byId.get(existing);
                            if (existRow != null && existRow.getIsSpecial() == 1 && r.getIsSpecial() == 0) {
                                byQ.put(r.getLevel(), r.getId());
                            }
                        }

                        byId.put(r.getId(), r);
                        ids.add(r.getId());
                    }
                });

                // Freeze (unmodifiable)
                pref.replaceAll((p, qMap) -> {
                    qMap.replaceAll((q, lvMap) -> Collections.unmodifiableMap(lvMap));
                    return Collections.unmodifiableMap(qMap);
                });
                all.replaceAll((p, qMap) -> {
                    qMap.replaceAll((q, lvMap) -> {
                        lvMap.replaceAll((lv, list) -> List.copyOf(list));
                        return Collections.unmodifiableMap(lvMap);
                    });
                    return Collections.unmodifiableMap(qMap);
                });

                idxPref.set(Collections.unmodifiableMap(pref));
                idxAll.set(Collections.unmodifiableMap(all));
                allEquipIds.set(Collections.unmodifiableSet(ids));
                metaById.set(Collections.unmodifiableMap(byId));

            } catch (Exception e) {
                throw new RuntimeException("Parse equipment.json failed", e);
            }
            if (resp.getHeaders().getETag() != null) etag.set(resp.getHeaders().getETag());

        } else if (resp.getStatusCode().value() == 304) {
            if (idxPref.get() == null || metaById.get() == null) {
                throw new IllegalStateException("304 on first load of equipment.json");
            }
        } else {
            if (idxPref.get() == null) {
                throw new IllegalStateException("Cannot load equipment.json (status " + resp.getStatusCode() + ")");
            }
        }
    }

    public void ensureLoaded() { loadIfNeeded(); }

    /** id hợp lệ trong equipment.json? */
    public boolean isEquipId(int itemId) {
        ensureLoaded();
        return allEquipIds.get().contains(itemId);
    }

    /** Trả về id "ưu tiên" cho (part, quality, level). Ưu tiên is_special=0 nếu có. */
    public Optional<Integer> resolve(int part, int quality, int level) {
        ensureLoaded();
        Map<Integer, Map<Integer, Integer>> byPart = idxPref.get().get(part);
        if (byPart == null) return Optional.empty();
        Map<Integer, Integer> byQ = byPart.get(quality);
        if (byQ == null) return Optional.empty();
        return Optional.ofNullable(byQ.get(level));
    }

    /** Trả về tất cả id cho (part, quality, level) – có thể gồm item special. */
    public List<Integer> resolveAll(int part, int quality, int level) {
        ensureLoaded();
        return idxAll.get()
                .getOrDefault(part, Map.of())
                .getOrDefault(quality, Map.of())
                .getOrDefault(level, List.of());
    }

    /** Tìm (part, quality, level) từ itemId. */
    public Optional<int[]> findPQLById(int itemId) {
        ensureLoaded();
        BoxDTOs.EquipRow r = metaById.get().get(itemId);
        if (r == null) return Optional.empty();
        return Optional.of(new int[]{ r.getPart(), r.getQuality(), r.getLevel() });
    }

    /** Lấy row theo id. */
    public Optional<BoxDTOs.EquipRow> rowOf(int itemId) {
        ensureLoaded();
        return Optional.ofNullable(metaById.get().get(itemId));
    }

    /** Stats chuẩn hoá: gồm range + attrType mặc định từ frist_att/second_att. */
    public Optional<BoxDTOs.EquipStats> statsOf(int itemId) {
        return rowOf(itemId).map(BoxDTOs.EquipStats::fromRow);
    }

    /** Nếu cần JSON stats để nhét thẳng vào pending.stats (giống screenshot: size=6). */
    public Optional<Map<String, Object>> statsJsonOf(int itemId) {
        return statsOf(itemId).map(BoxDTOs.EquipStats::toJson);
    }

    /** Map đúng y chang khóa trong equipment.json (kiểu số). */
    public Optional<Map<String, Object>> allFieldsRawOf(int itemId) {
        return rowOf(itemId).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",            r.getId());
            m.put("part",          r.getPart());
            m.put("level",         r.getLevel());
            m.put("quality",       r.getQuality());
            m.put("speed_min",     r.getSpeedMin());
            m.put("speed_max",     r.getSpeedMax());
            m.put("hp_min",        r.getHpMin());
            m.put("hp_max",        r.getHpMax());
            m.put("att_min",       r.getAttMin());
            m.put("att_max",       r.getAttMax());
            m.put("def_min",       r.getDefMin());
            m.put("def_max",       r.getDefMax());
            m.put("exp",           r.getExp());
            m.put("frist_att",     r.getFristAtt());
            m.put("second_att",    r.getSecondAtt());
            m.put("item_type",     r.getItemType());
            m.put("sellprice",     r.getSellPrice());
            m.put("pile_limit",    r.getPileLimit());
            m.put("isdroprecord",  r.getIsDropRecord());
            m.put("invalid_time",  r.getInvalidTime());
            m.put("is_special",    r.getIsSpecial() == 0 ? 1 : 0);
            return m;
        });
    }

    /** Giống raw nhưng mọi giá trị đều là String (in/log ra giống file). */
    public Optional<Map<String, String>> allFieldsRawStringsOf(int itemId) {
        return allFieldsRawOf(itemId).map(raw -> {
            Map<String, String> s = new LinkedHashMap<>();
            raw.forEach((k, v) -> s.put(k, String.valueOf(v)));
            return s;
        });
    }

    /** Bản canonical: stats gom {min,max}, và attr_type1/attr_type2 để sẵn trong stats. */
    public Optional<Map<String, Object>> allFieldsCanonicalOf(int itemId) {
        return rowOf(itemId).map(r -> {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("hp",     Map.of("min", r.getHpMin(),    "max", r.getHpMax()));
            stats.put("attack", Map.of("min", r.getAttMin(),   "max", r.getAttMax()));
            stats.put("defense",Map.of("min", r.getDefMin(),   "max", r.getDefMax()));
            stats.put("speed",  Map.of("min", r.getSpeedMin(), "max", r.getSpeedMax()));
            stats.put("attr_type1", (int) r.getFristAtt());
            stats.put("attr_type2", (int) r.getSecondAtt());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId",     r.getId());
            m.put("part",       r.getPart());
            m.put("level",      r.getLevel());
            m.put("quality",    r.getQuality());
            m.put("stats",      stats);
            m.put("exp",        r.getExp());
            m.put("item_type",  r.getItemType());
            m.put("sellprice",  r.getSellPrice());
            m.put("pile_limit", r.getPileLimit());
            m.put("isdroprecord", r.getIsDropRecord());
            m.put("invalid_time", r.getInvalidTime());
            m.put("is_special",   r.getIsSpecial() == 0 ? 1 : 0);
            return m;
        });
    }

    /** Expose only if cần debug. */
    public Map<Integer, Map<Integer, Map<Integer, Integer>>> getIdxPreferred() {
        ensureLoaded();
        return idxPref.get();
    }
}