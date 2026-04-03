package com.SouthMillion.role_service.config;

import com.SouthMillion.role_service.service.client.ConfigFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * RoleConfigCache
 * - Nạp & cache: roleexp.json (exp_config/base_att/other) + rolename.json (nếu có) qua ConfigFeign (ETag-aware).
 * - Cung cấp:
 *     + LevelTable: expRequiredFor(level), min/max, increment mỗi level (hp/atk/def/spd).
 *     + BaseAttrCfg: chỉ số nền từ base_att (hp/gongji/fangyu/speed).
 *     + OtherCfg: box_id/box_num từ other.
 *     + randomName(): ưu tiên rolename.json, fallback Player_xxxx.
 *
 * Property mẫu:
 *   role.config.roleexp-path-API: "/api/config/file?path=gameworld/logicconfig/roleexp.json"
 *   role.config.rolename-path-API: "/api/config/file?path=gameworld/logicconfig/rolename.json"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleConfigCache {

    private final ConfigFeign configFeign;
    private final StringRedisTemplate redis;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${role.config.redis-enabled:true}")
    private boolean redisEnabled;

    @Value("${role.config.redis-ttl-hours:24}")
    private long redisTtlHours;

    @Value("${role.config.startup-retry-count:6}")
    private int startupRetryCount;

    @Value("${role.config.startup-retry-delay-ms:2000}")
    private long startupRetryDelayMs;

    @Value("${role.config.roleexp-path:${role.config.roleexp-path-API:/api/config/file?path=gameworld/logicconfig/roleexp.json}}")
    private String roleExpPath;

    @Value("${role.config.rolename-path:${role.config.rolename-path-API:/api/config/file?path=gameworld/logicconfig/role_name.json}}")
    private String roleNamePath;

    // ===== ETags =====
    private final AtomicReference<String> etagRoleExp  = new AtomicReference<>(null);
    private final AtomicReference<String> etagRoleName = new AtomicReference<>(null);

    // ===== Data caches =====
    private final AtomicReference<LevelTable> levelTableRef = new AtomicReference<>(LevelTable.defaultTable());
    private final AtomicReference<BaseAttrCfg> baseAttrRef  = new AtomicReference<>(BaseAttrCfg.defaults());
    private final AtomicReference<OtherCfg> otherRef        = new AtomicReference<>(OtherCfg.defaults());
    private final AtomicReference<List<String>> namePoolRef = new AtomicReference<>(List.of("Player"));

    // Guard: ContextRefreshedEvent fires multiple times (parent/child contexts, Eureka re-register, etc.)
    private final java.util.concurrent.atomic.AtomicBoolean warmedUp = new java.util.concurrent.atomic.AtomicBoolean(false);

    /** Nạp ngay khi Spring context sẵn sàng, trước khi xử lý bất kỳ request nào */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextReady() {
        if (warmedUp.compareAndSet(false, true)) {
            warmupFromConfigService();
        }
    }

    /** Scheduler: nạp lại định kỳ (ETag sẽ giúp 304 Not Modified) */
    @Scheduled(initialDelay = 60000, fixedDelayString = "${role.config.refresh-interval-ms:60000}")
    public void refresh() {
        refreshRoleExp();
        refreshRoleName();
    }

    private void warmupFromConfigService() {
        String normalizedRoleExpPath = normalizeConfigPath(roleExpPath, "gameworld/logicconfig/roleexp.json");
        String normalizedRoleNamePath = normalizeConfigPath(roleNamePath, "gameworld/logicconfig/role_name.json");

        for (int attempt = 1; attempt <= startupRetryCount; attempt++) {
            boolean roleExpReady = refreshRoleExp(normalizedRoleExpPath);
            refreshRoleName(normalizedRoleNamePath);

            if (roleExpReady) {
                log.info("[RoleConfigCache] Connected to config-service and loaded role configs (attempt={}/{})",
                        attempt, startupRetryCount);
                return;
            }

            if (attempt < startupRetryCount) {
                log.warn("[RoleConfigCache] config-service not ready (attempt={}/{}), retry in {}ms",
                        attempt, startupRetryCount, startupRetryDelayMs);
                sleepQuietly(startupRetryDelayMs);
            }
        }

        log.warn("[RoleConfigCache] Startup retries exhausted, continue with cached/default role config until scheduler refresh succeeds");
    }

    // ========================= REFRESHERS =========================

    private void refreshRoleExp() {
        refreshRoleExp(normalizeConfigPath(roleExpPath, "gameworld/logicconfig/roleexp.json"));
    }

    private void refreshRoleName() {
        refreshRoleName(normalizeConfigPath(roleNamePath, "gameworld/logicconfig/role_name.json"));
    }

    /** Nạp roleexp.json (exp_config/base_att/other) với Redis-first strategy */
    private boolean refreshRoleExp(String path) {
        try {
            String redisKey = toRedisKey(path);
            String json = null;

            // 1. Try Redis first (if enabled)
            if (redisEnabled) {
                json = redis.opsForValue().get(redisKey);
                if (json != null && !json.isBlank()) {
                    log.debug("[RoleConfigCache] Redis HIT path={}", path);
                    RoleExpBundle bundle = parseRoleExpBundle(json);
                    levelTableRef.set(bundle.table());
                    baseAttrRef.set(bundle.baseAttr());
                    otherRef.set(bundle.other());
                    return true;
                }
                log.debug("[RoleConfigCache] Redis MISS path={}, calling config-service", path);
            }

            // 2. Redis miss or disabled → call config-service
            ResponseEntity<byte[]> res = configFeign.getFile(path, etagRoleExp.get());
            if (res.getStatusCode() == HttpStatus.NOT_MODIFIED) return true;

            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                json = new String(res.getBody(), StandardCharsets.UTF_8);

                RoleExpBundle bundle = parseRoleExpBundle(json);

                levelTableRef.set(bundle.table());
                baseAttrRef.set(bundle.baseAttr());
                otherRef.set(bundle.other());

                String et = res.getHeaders().getFirst(HttpHeaders.ETAG);
                if (StringUtils.hasText(et)) etagRoleExp.set(et);

                // 3. Cache in Redis for next time
                if (redisEnabled && json != null) {
                    redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
                }

                log.info("[RoleConfigCache] roleexp.json loaded: {} levels, baseAtt(hp={},atk={},def={},spd={}), other(boxId={},boxNum={})",
                        bundle.table().maxLevel(),
                        bundle.baseAttr().getHp(), bundle.baseAttr().getAttack(), bundle.baseAttr().getDefend(), bundle.baseAttr().getSpeed(),
                        bundle.other().getBoxId(), bundle.other().getBoxNum());
                return true;
            }
        } catch (FeignException ex) {
            if (ex.status() == 304) return true; // Not Modified — dữ liệu cache vẫn hợp lệ
            log.warn("[RoleConfigCache] Cannot refresh roleexp.json (path={}), cause={}", path, ex.toString());
        } catch (Exception ex) {
            log.warn("[RoleConfigCache] Cannot refresh roleexp.json (path={}), cause={}", path, ex.toString());
        }
        return false;
    }

    /** Nạp role_name.json (nếu có) với Redis-first strategy */
    private boolean refreshRoleName(String path) {
        try {
            String redisKey = toRedisKey(path);
            String json = null;

            // 1. Try Redis first (if enabled)
            if (redisEnabled) {
                json = redis.opsForValue().get(redisKey);
                if (json != null && !json.isBlank()) {
                    log.debug("[RoleConfigCache] Redis HIT path={}", path);
                    List<String> names = parseNamePool(json);
                    if (names != null && !names.isEmpty()) {
                        namePoolRef.set(names);
                        return true;
                    }
                }
                log.debug("[RoleConfigCache] Redis MISS path={}, calling config-service", path);
            }

            // 2. Redis miss or disabled → call config-service
            ResponseEntity<byte[]> res = configFeign.getFile(path, etagRoleName.get());
            if (res.getStatusCode() == HttpStatus.NOT_MODIFIED) return true;

            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                json = new String(res.getBody(), StandardCharsets.UTF_8);
                List<String> names = parseNamePool(json);
                if (names != null && !names.isEmpty()) {
                    namePoolRef.set(names);
                    String et = res.getHeaders().getFirst(HttpHeaders.ETAG);
                    if (StringUtils.hasText(et)) etagRoleName.set(et);

                    // 3. Cache in Redis for next time
                    if (redisEnabled && json != null) {
                        redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
                    }

                    log.info("[RoleConfigCache] role_name.json loaded: {} names", names.size());
                    return true;
                }
                return true;
            }
        } catch (FeignException ex) {
            if (ex.status() == 304) return true; // Not Modified — dữ liệu cache vẫn hợp lệ
            log.warn("[RoleConfigCache] Cannot refresh role_name.json (path={}), cause={}", path, ex.toString());
        } catch (Exception ex) {
            log.warn("[RoleConfigCache] Cannot refresh role_name.json (path={}), cause={}", path, ex.toString());
        }
        return false;
    }

    // ========================= PARSERS =========================

    /** Holder cho 3 phần trong roleexp.json */
    private record RoleExpBundle(LevelTable table, BaseAttrCfg baseAttr, OtherCfg other) {}

    /** Parse đúng schema: exp_config (array), base_att (array hoặc object), other (array hoặc object). */
    private RoleExpBundle parseRoleExpBundle(String json) {
        try {
            JsonNode root = om.readTree(json);

            // 1) EXP CONFIG
            Map<Integer, Long> lvlReq = new HashMap<>();
            JsonNode expArr = root.get("exp_config");
            if (expArr != null && expArr.isArray()) {
                for (JsonNode node : expArr) {
                    Integer lv = optIntNode(node, "level");
                    Long exp = optLongNode(node, "exp");
                    if (lv != null && exp != null) lvlReq.put(lv, exp);
                }
            } else {
                // Fallback: hỗ trợ định dạng cũ "levels" hoặc map {"1":100,...}
                Map<String, Object> raw = om.readValue(json, new TypeReference<Map<String, Object>>() {});
                if (raw.containsKey("levels")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> arr = (List<Map<String, Object>>) raw.get("levels");
                    for (Map<String, Object> obj : arr) {
                        Integer lv = toInt(obj.get("level"));
                        Long exp = toLong(obj.get("exp"));
                        if (lv != null && exp != null) lvlReq.put(lv, exp);
                    }
                } else {
                    for (Map.Entry<String, Object> e : raw.entrySet()) {
                        Integer lv = tryParseInt(e.getKey());
                        Long exp = toLong(e.getValue());
                        if (lv != null && exp != null) lvlReq.put(lv, exp);
                    }
                }
            }
            if (lvlReq.isEmpty()) {
                // Trả bảng mặc định an toàn để service không crash
                return new RoleExpBundle(LevelTable.defaultTable(), BaseAttrCfg.defaults(), OtherCfg.defaults());
            }
            int maxLv = lvlReq.keySet().stream().max(Integer::compareTo).orElse(60);
            LevelTable table = new LevelTable(lvlReq, 1, maxLv);

            // 2) BASE ATTR
            BaseAttrCfg base = BaseAttrCfg.defaults();
            JsonNode baseNode = root.get("base_att");
            if (baseNode != null) {
                JsonNode obj = baseNode.isArray() && baseNode.size() > 0 ? baseNode.get(0)
                        : baseNode.isObject() ? baseNode : null;
                if (obj != null && obj.isObject()) {
                    int speed  = optIntNode(obj, "speed", 0);
                    int hp     = optIntNode(obj, "hp", 0);
                    int gongji = optIntNode(obj, "gongji", 0);
                    int fangyu = optIntNode(obj, "fangyu", 0);
                    base = new BaseAttrCfg(speed, hp, gongji, fangyu);
                }
            }

            // 3) OTHER
            OtherCfg other = OtherCfg.defaults();
            JsonNode otherNode = root.get("other");
            if (otherNode != null) {
                JsonNode obj = otherNode.isArray() && otherNode.size() > 0 ? otherNode.get(0)
                        : otherNode.isObject() ? otherNode : null;
                if (obj != null && obj.isObject()) {
                    long boxNum = optLongNode(obj, "box_num", 0L);
                    int  boxId  = optIntNode(obj, "box_id", 0);
                    other = new OtherCfg(boxNum, boxId);
                }
            }

            return new RoleExpBundle(table, base, other);

        } catch (Exception e) {
            log.warn("[RoleConfigCache] parseRoleExpBundle failed, cause={}", e.toString());
            return new RoleExpBundle(LevelTable.defaultTable(), BaseAttrCfg.defaults(), OtherCfg.defaults());
        }
    }

    private List<String> parseNamePool(String json) {
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                return om.readValue(json, new TypeReference<List<String>>() {});
            }
            Map<String, Object> m = om.readValue(json, new TypeReference<Map<String, Object>>() {});
            Object val = m.getOrDefault("names", m.get("data"));
            if (val instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) if (o != null) out.add(o.toString());
                return out;
            }
            return List.of("Player");
        } catch (Exception e) {
            log.warn("[RoleConfigCache] parseNamePool failed, cause={}", e.toString());
            return List.of("Player");
        }
    }

    // ========================= PUBLIC API =========================

    public LevelTable levelTable() { return levelTableRef.get(); }
    public BaseAttrCfg baseAttr()  { return baseAttrRef.get(); }
    public OtherCfg other()        { return otherRef.get(); }

    /** random name: ưu tiên pool từ rolename.json; thiếu thì "Player_xxxx" */
    public String randomName() {
        var pool = namePoolRef.get();
        String prefix = (pool == null || pool.isEmpty())
                ? "Player"
                : pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        String suffix = Integer.toString(ThreadLocalRandom.current().nextInt(1000, 10000));
        return prefix + "_" + suffix;
    }

    // ========================= MODELS =========================

    @Getter
    public static class LevelTable {
        private final Map<Integer, Long> expToNextLevel;
        private final int minLevel;
        private final int maxLevel;
        private final long hpPerLv;
        private final long atkPerLv;
        private final long defPerLv;
        private final int  spdPerLv;

        public LevelTable(Map<Integer, Long> expToNextLevel, int minLevel, int maxLevel) {
            this(expToNextLevel, minLevel, maxLevel, 10L, 2L, 2L, 1);
        }
        public LevelTable(Map<Integer, Long> expToNextLevel, int minLevel, int maxLevel,
                          long hpPerLv, long atkPerLv, long defPerLv, int spdPerLv) {
            this.expToNextLevel = Map.copyOf(expToNextLevel);
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.hpPerLv = hpPerLv;
            this.atkPerLv = atkPerLv;
            this.defPerLv = defPerLv;
            this.spdPerLv = spdPerLv;
        }

        /** Không auto-fallback: thiếu exp => trả giá trị rất lớn để chặn lên cấp. */
        public long expRequiredFor(int level) {
            Long v = expToNextLevel.get(level);
            return (v != null) ? v : Long.MAX_VALUE / 4;
        }

        /** Giữ lại tên method cũ để tương thích với chỗ dùng table.maxLevel(). */
        public int maxLevel() { return maxLevel; }

        public static LevelTable defaultTable() {
            Map<Integer, Long> m = new HashMap<>();
            for (int lv = 1; lv <= 60; lv++) m.put(lv, 100L + 50L * lv);
            return new LevelTable(m, 1, 60);
        }
    }

    @Getter
    public static class BaseAttrCfg {
        private final int speed;
        private final int hp;
        private final int attack;
        private final int defend;

        public BaseAttrCfg(int speed, int hp, int attack, int defend) {
            this.speed = speed;
            this.hp = hp;
            this.attack = attack;
            this.defend = defend;
        }

        public static BaseAttrCfg defaults() {
            // default nhẹ để tránh crash nếu thiếu config
            return new BaseAttrCfg(0, 100, 10, 5);
        }
    }

    @Getter
    public static class OtherCfg {
        private final long boxNum;
        private final int boxId;

        public OtherCfg(long boxNum, int boxId) {
            this.boxNum = boxNum;
            this.boxId = boxId;
        }

        public static OtherCfg defaults() { return new OtherCfg(0L, 0); }
    }

    // ========================= HELPERS =========================

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    private Integer tryParseInt(String s) {
        try { return Integer.valueOf(s); } catch (Exception e) { return null; }
    }

    private Integer optIntNode(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        if (n.isInt() || n.isLong()) return n.intValue();
        try { return Integer.valueOf(n.asText()); } catch (Exception ignore) { return null; }
    }

    private int optIntNode(JsonNode node, String field, int defVal) {
        Integer v = optIntNode(node, field);
        return v == null ? defVal : v;
    }

    private Long optLongNode(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.longValue();
        try { return Long.valueOf(n.asText()); } catch (Exception ignore) { return null; }
    }

    private long optLongNode(JsonNode node, String field, long defVal) {
        Long v = optLongNode(node, field);
        return v == null ? defVal : v;
    }

    private String normalizeConfigPath(String rawPath, String defaultPath) {
        if (!StringUtils.hasText(rawPath)) {
            return defaultPath;
        }

        String trimmed = rawPath.trim();
        if (!trimmed.contains("?")) {
            return trimmed;
        }

        try {
            String pathParam = UriComponentsBuilder.fromUriString(trimmed)
                    .build()
                    .getQueryParams()
                    .getFirst("path");
            return StringUtils.hasText(pathParam) ? pathParam : trimmed;
        } catch (Exception ex) {
            log.warn("[RoleConfigCache] Invalid config path format '{}', use raw path", rawPath);
            return trimmed;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Convert config path to Redis key format.
     * Example: "gameworld/logicconfig/roleexp.json" → "cfg:file:gameworld:logicconfig:roleexp.json"
     */
    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }
}