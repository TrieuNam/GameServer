package com.southMillion.webSocket_server.handler.box;


import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.service.client.BoxFeign;
import com.southMillion.webSocket_server.utils.FeignCall;
import com.southMillion.webSocket_server.utils.SessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BoxHandler:
 * - Lắng nghe CS_BOX_REQ
 * - Map req_type theo C++ client:
 *      1: Open
 *      2: Wear
 *      3: Sell
 *      4: Buy
 *      5: LevelUp
 * - Sau mỗi thao tác: luôn refresh SC_BOX_INFO (+ SC_BOX_SETING_INFO nếu có API)
 * - Khi Open xong: nếu response có equip mới -> emit SC_BOX_EQUIP_INFO
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoxHandler implements MessageHandler {

    private final BoxFeign boxFeign;

    @Override
    public int[] interests() {
        return new int[]{ MsgIds.CS_BOX_REQ };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (ps == null || !StringUtils.hasText(ps.getRoleId())) {
            return Mono.empty();
        }

        final Msgbox.PB_CSBoxReq req;
        try {
            req = Msgbox.PB_CSBoxReq.parseFrom(payload);
        } catch (Exception e) {
            log.warn("[box] cannot parse PB_CSBoxReq: {}", e.toString());
            return Mono.empty();
        }

        final int op = req.hasReqType() ? req.getReqType() : 0;

        return switch (op) {
            case 1 -> onOpen(ps, req);
            case 2 -> onWear(ps);
            case 3 -> onSell(ps);
            case 4 -> onBuy(ps);
            case 5 -> onLevelUp(ps);
            default -> {
                log.warn("[box] unknown req_type={} (ignore)", op);
                yield Mono.empty();
            }
        };
    }

    // ========================= Operations =========================

    private Mono<Void> onOpen(PlayerSession ps, Msgbox.PB_CSBoxReq req) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();

        int count = req.hasParam() ? req.getParam() : 1;
        if (count < 1) count = 1;
        if (count > 5) count = 5;

        int roleLevel = SessionUtils.roleLevelOrDefault(ps, 1);

        var openReq = new BoxDTOs.OpenReq();
        openReq.setRoleId(rid);
        openReq.setCount(count);
        openReq.setRoleLevel(roleLevel);

        int finalCount = count;
        return FeignCall.withToken(tk, "box.open", () -> boxFeign.open(openReq))
                .doOnNext(open -> {
                    log.debug("[box] open ok rid={} count={} resp(openBoxTotal={}, lastOpenIsFive={}, hasPending={})",
                            rid, finalCount,
                            (open != null ? open.getOpenBoxTotal() : null),
                            (open != null && Boolean.TRUE.equals(open.isLastOpenIsFive())),
                            hasNonNull(open, "getPending", "getEquip", "getEquipInfo", "getPendingEquip"));

                    // Nếu response có equip mới => emit SC_BOX_EQUIP_INFO (best-effort)
                    tryEmitEquipAfterOpen(ps, open);
                })
                .onErrorResume(ex -> {
                    log.warn("[box] open ERROR rid={} count={} ex={}", rid, finalCount, ex.toString());
                    return Mono.empty();
                })
                .then(refreshBoxPanels(tk, rid, ps));
    }

    private Mono<Void> onWear(PlayerSession ps) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();

        var req = new BoxDTOs.WearReq();
        req.setRoleId(rid);

        return FeignCall.withToken(tk, "box.wear", () -> boxFeign.wear(req))
                .doOnNext(resp -> log.debug("[box] wear ok rid={}", rid))
                .onErrorResume(ex -> {
                    log.warn("[box] wear ERROR rid={} ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                // bắn EQUIP_INFO và 2 panel song song
                .then(Mono.whenDelayError(
                        refreshBoxEquip(tk, rid, ps),
                        refreshBoxPanels(tk, rid, ps)
                ));
    }

    private Mono<Void> onSell(PlayerSession ps) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();

        var req = new BoxDTOs.SellReq();
        req.setRoleId(rid);

        return FeignCall.withToken(tk, "box.sell", () -> boxFeign.sell(req))
                .doOnNext(resp -> log.debug("[box] sell ok rid={}", rid))
                .onErrorResume(ex -> {
                    log.warn("[box] sell ERROR rid={} ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then(refreshBoxPanels(tk, rid, ps));
    }

    private Mono<Void> onBuy(PlayerSession ps) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();

        var req = new BoxDTOs.SimpleReq();
        req.setRoleId(rid);

        return FeignCall.withToken(tk, "box.buy", () -> boxFeign.buy(req))
                .doOnNext(resp -> log.debug("[box] buy ok rid={}", rid))
                .onErrorResume(ex -> {
                    log.warn("[box] buy ERROR rid={} ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then(refreshBoxPanels(tk, rid, ps));
    }

    private Mono<Void> onLevelUp(PlayerSession ps) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();

        var req = new BoxDTOs.SimpleReq();
        req.setRoleId(rid);

        return FeignCall.withToken(tk, "box.levelUp", () -> boxFeign.levelUp(req))
                .doOnNext(resp -> log.debug("[box] level-up ok rid={}", rid))
                .onErrorResume(ex -> {
                    log.warn("[box] level-up ERROR rid={} ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then(refreshBoxPanels(tk, rid, ps));
    }

    // ========================= Helpers =========================
    private Mono<Void> refreshBoxEquip(String token, String rid, PlayerSession ps) {
        return FeignCall.withToken(token, "box.equipInfo", () -> boxFeign.equipInfo(rid))
                .doOnNext(eq -> {
                    try { Emitters.sendBoxEquipInfo(ps, eq); } catch (Throwable t) {
                        log.warn("[box] sendBoxEquipInfo emit error rid={} ex={}", rid, t.toString());
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("[box] box.equipInfo ERROR rid={} ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then();
    }


    /**
     * Luôn refresh lại UI Box:
     * - SC_BOX_INFO (bắt buộc)
     * - SC_BOX_SETING_INFO (nếu có API /getSetting)
     */
    private Mono<Void> refreshBoxPanels(String token, String rid, PlayerSession ps) {
        Mono<Void> infoMono = FeignCall.withToken(token, "box.info", () -> boxFeign.info(rid))
                .doOnNext(info -> {
                    if (info != null) {
                        try { Emitters.sendBoxInfo(ps, info); } catch (Throwable t) {
                            log.warn("[box] sendBoxInfo emit error rid={} ex={}", rid, t.toString());
                        }
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("[box] box.info ERROR rid={} ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then();

        Mono<Void> settingMono = FeignCall.withToken(token, "box.getSetting", () -> boxFeign.getSetting(rid))
                .doOnNext(set -> {
                    if (set != null) {
                        try { Emitters.sendBoxSettingInfo(ps, set); } catch (Throwable t) {
                            log.warn("[box] sendBoxSettingInfo emit error rid={} ex={}", rid, t.toString());
                        }
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("[box] box.getSetting ERROR rid={} ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then();

        return Mono.whenDelayError(infoMono, settingMono).then();
    }

    /**
     * Best-effort: cố gắng lấy equip/pending equip từ OpenResp và emit SC_BOX_EQUIP_INFO.
     * Không ràng buộc DTO cụ thể; dùng reflection với các tên getter phổ biến.
     */
    private void tryEmitEquipAfterOpen(PlayerSession ps, Object openResp) {
        if (ps == null || openResp == null) return;

        // Trường hợp chuẩn: OpenResp có getPending(): Map<String,Object>
        if (openResp instanceof BoxDTOs.OpenResp r) {
            emitEquipFromPendingMap(ps, r.getPending(), /*isNew*/ true);
            return;
        }

        // Fallback cực đoan: thử lấy phương thức getPending() nếu có (không log lỗi)
        try {
            var m = openResp.getClass().getMethod("getPending").invoke(openResp);
            if (m instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) map;
                emitEquipFromPendingMap(ps, cast, /*isNew*/ true);
            }
        } catch (Throwable ignore) {
            // nuốt lỗi để khỏi spam log
        }
    }

    @SuppressWarnings("unchecked")
    private void emitEquipFromPendingMap(PlayerSession ps, Map<String, Object> pending, boolean isNew) {
        if (ps == null || pending == null || pending.isEmpty()) return;

        // ---- chuẩn hóa source map như cũ ----
        Map<String, Object> src = pending;
        Object nested = pending.get("equip");
        if (nested instanceof Map<?,?> m) {
            src = (Map<String,Object>) m;
        }

        Integer itemId   = pickInt(src, "itemId", "item_id", "id");
        Integer equipTyp = pickInt(src, "equipType", "equip_type", "type", "pos");
        if (itemId == null || itemId <= 0) return;

        // ---- Lấy equipMeta / equipMetaRaw / idxPrefId từ pending ----
        Map<String, Object> equipMeta    = pickMap(src, "equipMeta");
        if (equipMeta == null)    equipMeta    = pickMap(pending, "equipMeta");

        Map<String, Object> equipMetaRaw = pickMap(src, "equipMetaRaw");
        if (equipMetaRaw == null) equipMetaRaw = pickMap(pending, "equipMetaRaw");

        Integer idxPrefId = pickInt(src, "idxPrefId", "idx_pref_id");
        if (idxPrefId == null) idxPrefId = pickInt(pending, "idxPrefId", "idx_pref_id");

        // equipType fallback từ equipMetaRaw.part nếu thiếu
        if (equipTyp == null) {
            Integer partFromMeta = parseIntSafe(equipMetaRaw != null ? equipMetaRaw.get("part") : null);
            equipTyp = (partFromMeta != null) ? partFromMeta : 1;
        }

        // ---- Lấy stats (ưu tiên pending.stats → equipMeta.stats) ----
        Map<String, Object> stats = pickMap(src, "stats");
        if (stats == null) stats = pickMap(pending, "stats");
        Map<String, Object> metaStats = (equipMeta != null) ? pickMap(equipMeta, "stats") : null;

        // ---- Resolve giá trị số cho PB: stats → metaStats → equipMetaRaw ----
        Integer hpVal    = firstNonNull(
                getStatAsInt(stats, "hp"),                 readRangeOrNumber(stats, "hp"),
                getStatAsInt(metaStats, "hp"),             readRangeOrNumber(metaStats, "hp"),
                chooseFromMetaRaw(equipMetaRaw, "hp_min", "hp_max")
        );
        Integer atkVal   = firstNonNull(
                getStatAsInt(stats, "attack","att","atk","actack"), readRangeOrNumber(stats, "attack"),
                getStatAsInt(metaStats, "attack"),                  readRangeOrNumber(metaStats, "attack"),
                chooseFromMetaRaw(equipMetaRaw, "att_min", "att_max")
        );
        Integer defVal   = firstNonNull(
                getStatAsInt(stats, "defense","def","defend"), readRangeOrNumber(stats, "defense"),
                getStatAsInt(metaStats, "defense"),            readRangeOrNumber(metaStats, "defense"),
                chooseFromMetaRaw(equipMetaRaw, "def_min", "def_max")
        );
        Integer speedVal = firstNonNull(
                getStatAsInt(stats, "speed"),              readRangeOrNumber(stats, "speed"),
                getStatAsInt(metaStats, "speed"),          readRangeOrNumber(metaStats, "speed"),
                chooseFromMetaRaw(equipMetaRaw, "speed_min", "speed_max")
        );

        // ---- attr types/values: stats → metaStats → equipMetaRaw (frist/second_att) ----
        Integer attrType1  = firstNonNull(
                getStatAsInt(stats, "attr_type1","attrType1","first_att","frist_att"),
                getStatAsInt(metaStats, "attr_type1"),
                parseIntSafe(equipMetaRaw != null ? equipMetaRaw.get("frist_att") : null)
        );
        Integer attrType2  = firstNonNull(
                getStatAsInt(stats, "attr_type2","attrType2","second_att"),
                getStatAsInt(metaStats, "attr_type2"),
                parseIntSafe(equipMetaRaw != null ? equipMetaRaw.get("second_att") : null)
        );
        Integer attrValue1 = firstNonNull(getStatAsInt(stats, "attr_value1","attrValue1"),
                getStatAsInt(metaStats, "attr_value1"));
        Integer attrValue2 = firstNonNull(getStatAsInt(stats, "attr_value2","attrValue2"),
                getStatAsInt(metaStats, "attr_value2"));

        // ---- Persist pendingJson: giữ nguyên các block đã có ----
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("kind", "equip");
        canonical.put("equip", Map.of("itemId", itemId, "equipType", equipTyp));
        if (stats != null && !stats.isEmpty()) canonical.put("stats", stats);
        if (equipMeta != null && !equipMeta.isEmpty()) canonical.put("equipMeta", equipMeta);
        if (equipMetaRaw != null && !equipMetaRaw.isEmpty()) canonical.put("equipMetaRaw", equipMetaRaw);
        if (idxPrefId != null) canonical.put("idxPrefId", idxPrefId);

        try {
            BoxDTOs.SetPendingReq req = BoxDTOs.SetPendingReq.builder()
                    .roleId(ps.getRoleId())
                    .pending(canonical)
                    .build();
            FeignCall.withToken(ps.getSessionId(), () -> boxFeign.setPending(req));
        } catch (Throwable t) {
            log.warn("[box] persist pendingJson failed: {}", t.toString());
        }

        // ---- Build & emit PB ----
        Msgequip.PB_EquipData.Builder equipB = Msgequip.PB_EquipData.newBuilder()
                .setItemId(itemId)
                .setEquipType(equipTyp);
        if (hpVal    != null) equipB.setHp(hpVal);
        if (atkVal   != null) equipB.setAttack(atkVal);
        if (defVal   != null) equipB.setDefend(defVal);
        if (speedVal != null) equipB.setSpeed(speedVal);
        if (attrType1  != null) equipB.setAttrType1(attrType1);
        if (attrType2  != null) equipB.setAttrType2(attrType2);
        if (attrValue1 != null) equipB.setAttrValue1(attrValue1);
        if (attrValue2 != null) equipB.setAttrValue2(attrValue2);

        var sc = Msgbox.PB_SCBoxEquipInfo.newBuilder()
                .setIsNew(isNew ? 1 : 0)
                .setEquipInfo(equipB.build())
                .build();
        Emitters.emit(ps, MsgIds.SC_BOX_EQUIP_INFO, sc.toByteArray());
    }

    /* ================= helpers (không cần EquipmentIndex) ================= */

    private static Integer firstNonNull(Integer... arr) {
        for (Integer v : arr) if (v != null) return v;
        return null;
    }

    private static Integer parseIntSafe(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    /** stats[key] có thể là số hoặc {min,max}; ưu tiên max (đổi sang avg nếu muốn) */
    private static Integer readRangeOrNumber(Map<String,Object> stats, String key) {
        if (stats == null) return null;
        Object v = stats.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof Map<?,?> mm) {
            Object ma = mm.get("max");
            Object mi = mm.get("min");
            Integer vmax = parseIntSafe(ma);
            if (vmax != null) return vmax;
            return parseIntSafe(mi);
        }
        return parseIntSafe(v);
    }

    /** Lấy số từ equipMetaRaw: ưu tiên *_max → *_min */
    private static Integer chooseFromMetaRaw(Map<String,Object> raw, String minKey, String maxKey) {
        if (raw == null) return null;
        Integer vmax = parseIntSafe(raw.get(maxKey));
        if (vmax != null) return vmax;
        return parseIntSafe(raw.get(minKey));
    }



    /* ================= Helpers ================= */

    /** Lấy Map con theo các key (trả về null nếu không có hoặc không phải Map). */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> pickMap(Map<String, Object> m, String... keys) {
        if (m == null) return null;
        for (String k : keys) {
            Object v = m.get(k);
            if (v instanceof Map<?, ?> mm) return (Map<String, Object>) mm;
        }
        return null;
    }

    /** Lấy int theo danh sách key ưu tiên (int/long/string số/Map {value} hoặc {min,max}). */
    private static Integer getStatAsInt(Map<String, Object> stats, String... keys) {
        if (stats == null) return null;
        for (String k : keys) {
            Object node = stats.get(k);
            Integer val = coerceToInt(node);
            if (val != null) return val;
        }
        return null;
    }

    private static Integer coerceToInt(Object node) {
        if (node == null) return null;
        if (node instanceof Number n) return safeInt(n.longValue());
        if (node instanceof String s) {
            try { return safeInt(Long.parseLong(s.trim())); } catch (Exception ignore) {}
        }
        if (node instanceof Map<?, ?> any) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) any;
            // Ưu tiên "value" nếu có
            Integer v = coerceToInt(m.get("value"));
            if (v != null) return v;
            // Nếu có min/max → lấy trung bình (có thể đổi sang max nếu bạn muốn)
            Integer min = coerceToInt(m.get("min"));
            Integer max = coerceToInt(m.get("max"));
            if (min != null || max != null) {
                long a = (min != null ? min : 0);
                long b = (max != null ? max : a);
                return safeInt(a + ((b - a) / 2)); // avg
            }
        }
        return null;
    }

    private static Integer safeInt(long v) {
        if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) v;
    }

    private static Integer pickInt(Map<String, Object> m, String... keys) {
        if (m == null) return null;
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) continue;
            if (v instanceof Number n) return n.intValue();
            try { return Integer.parseInt(v.toString().trim()); } catch (Exception ignore) {}
        }
        return null;
    }


    private static boolean hasNonNull(Object obj, String... getters) {
        for (String g : getters) {
            if (safeInvoke(obj, g) != null) return true;
        }
        return false;
    }

    private static Object firstNonNull(Object... xs) {
        for (Object x : xs) if (x != null) return x;
        return null;
    }

    private static Object safeInvoke(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (Throwable e) { return null; }
    }

    private static Integer tryInt(Object target, String... methods) {
        for (String m : methods) {
            try {
                Object v = target.getClass().getMethod(m).invoke(target);
                if (v == null) continue;
                if (v instanceof Number n) return n.intValue();
                return Integer.parseInt(String.valueOf(v));
            } catch (Throwable ignore) {}
        }
        return null;
    }




}
