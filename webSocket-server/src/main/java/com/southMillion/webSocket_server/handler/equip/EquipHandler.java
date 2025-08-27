package com.southMillion.webSocket_server.handler.equip;

import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.service.client.BagPublicHttpClient;
import com.southMillion.webSocket_server.service.client.EquipFumoFeign;
import com.southMillion.webSocket_server.service.client.EquipHttpClient;
import com.southMillion.webSocket_server.service.client.ItemMetaFeign;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.IntFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class EquipHandler implements MessageHandler {

    private final EquipHttpClient equipFeign;
    private final EquipFumoFeign  equipFumoFeign;
    private final BagPublicHttpClient bagPublic;
    private final ItemMetaFeign itemMetaFeign;

    private static final byte BAG_EQUIP = 1;

    @Override
    public int[] interests() { return new int[]{ MsgIds.CS_EQUIP_REQ }; }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();

        Msgequip.PB_CSEquipReq req;
        try {
            req = Msgequip.PB_CSEquipReq.parseFrom(payload);
        } catch (Exception e) {
            log.warn("PB_CSEquipReq parse error: {}", e.toString());
            return Mono.empty();
        }

        final int op = req.hasReqType() ? req.getReqType() : 0;

        return switch (op) {
            case 1  -> onWear(ps, req);         // mặc itemId từ túi: param1=itemId
            case 11 -> onFumoList(ps);          // lấy toàn bộ fumo
            case 12 -> onFumoOne(ps, req);      // lấy 1 slot fumo: param1=equipType
            case 13 -> onFumoAddExp(ps, req);   // add exp: param1=equipType, param2=addExp
            case 14 -> onFumoActivate(ps, req); // activate: param1=equipType, param2=endTimeEpochSec
            case 15 -> onFumoReset(ps, req);    // reset: param1=equipType
            default -> Mono.empty();
        };
    }

    /** op=1: mặc itemId từ túi -> refresh equip-list & equip-bag */
    private Mono<Void> onWear(PlayerSession ps, Msgequip.PB_CSEquipReq req) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();
        final int itemId = req.hasParam1() ? req.getParam1() : 0;
        if (itemId <= 0) return Mono.empty();

        return FeignCall.withToken(tk, "equip.wear", () -> equipFeign.wear(rid, itemId))
                .onErrorResume(ex -> {
                    log.warn("equip.wear ERROR rid={}, itemId={}, ex={}", rid, itemId, ex.toString());
                    return Mono.empty();
                })
                // lấy lại danh sách trang bị đang mặc
                .then(Mono.defer(() ->
                        FeignCall.withToken(tk, "equip.list", () -> equipFeign.list(rid))
                                .doOnNext(list -> Emitters.sendEquipList(ps, list))
                                .onErrorResume(ex -> {
                                    log.warn("equip.list ERROR rid={}, ex={}", rid, ex.toString());
                                    return Mono.empty();
                                })
                ))
                // và túi Equip
                .then(Mono.defer(() ->
                        FeignCall.withToken(tk, "bag.get.equip", () -> bagPublic.get(rid, BAG_EQUIP))
                                .flatMap(bag -> sendEquipBagWithMeta(ps, tk, bag)) // <-- dùng helper như trên
                                .onErrorResume(ex -> {
                                    log.warn("bag.get.equip ERROR rid={}, ex={}", rid, ex.toString());
                                    return Mono.empty();
                                })
                ))
                .then();
    }

    private Mono<Void> sendEquipBagWithMeta(PlayerSession ps, String token, BagDTOs.BagView bag) {
        if (bag == null || bag.getSlots() == null || bag.getSlots().isEmpty()) {
            Emitters.sendEquipBagList(ps, bag, id -> 1);
            return Mono.empty();
        }
        String csv = toCsvDistinctItemIds(bag);
        if (csv.isBlank()) {
            Emitters.sendEquipBagList(ps, bag, id -> 1);
            return Mono.empty();
        }

        return FeignCall.withToken(token, "config.itemMetaBatch", () -> itemMetaFeign.batchMeta(csv))
                .onErrorResume(ex -> {
                    log.warn("[bootstrap] itemMetaBatch ERROR: {}", ex.toString());
                    return Mono.just(java.util.Collections.emptyMap());
                })
                .doOnNext(metas -> {
                    IntFunction<Integer> lookup = buildEquipTypeLookupFromMeta(metas);
                    Emitters.sendEquipBagList(ps, bag, lookup);
                })
                .then();
    }

    private IntFunction<Integer> buildEquipTypeLookupFromMeta(Map<String, Map<String, Object>> metas) {
        // map itemId -> equipType/pos
        var map = new java.util.HashMap<Integer, Integer>(metas != null ? metas.size() : 16);
        if (metas != null) {
            for (var e : metas.entrySet()) {
                try {
                    int itemId = Integer.parseInt(e.getKey());
                    Integer pos = pickInt(e.getValue(), "equipType", "equip_type", "type", "pos", "slot");
                    if (pos != null && pos > 0) map.put(itemId, pos);
                } catch (Exception ignore) {}
            }
        }
        return (int id) -> map.getOrDefault(id, 1);
    }

    private static Integer pickInt(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) continue;
            try {
                if (v instanceof Number n) {
                    long lv = (long)Math.floor(n.doubleValue());
                    if (lv > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                    if (lv < Integer.MIN_VALUE) return Integer.MIN_VALUE;
                    return (int) lv;
                }
                String s = String.valueOf(v).trim();
                if (s.isEmpty() || "null".equalsIgnoreCase(s)) continue;
                int dot = s.indexOf('.');
                if (dot > 0) s = s.substring(0, dot);
                return Integer.parseInt(s);
            } catch (Exception ignore) {}
        }
        return null;
    }

    private String toCsvDistinctItemIds(BagDTOs.BagView bag) {
        return bag.getSlots().stream()
                .map(s -> String.valueOf(s.getItemId()))
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
    }

    /** op=11: lấy toàn bộ Fumo */
    private Mono<Void> onFumoList(PlayerSession ps) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();

        return FeignCall.withToken(tk, "fumo.list", () -> equipFumoFeign.list(rid))
                .doOnNext(resp -> {
                    try { Emitters.sendEquipFumoList(ps, resp); }
                    catch (Throwable t) { log.warn("emit fumo list err: {}", t.toString()); }
                })
                .onErrorResume(ex -> {
                    log.warn("fumo.list ERROR rid={}, ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then();
    }



    /** op=12: 1 slot Fumo theo equipType=param1 */
    private Mono<Void> onFumoOne(PlayerSession ps, Msgequip.PB_CSEquipReq req) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();
        final int equipType = req.hasParam1() ? req.getParam1() : 0;
        if (equipType <= 0) return Mono.empty();

        return FeignCall.withToken(tk, "fumo.one", () -> equipFumoFeign.one(rid, equipType))
                .doOnNext(resp -> {
                    try { Emitters.sendEquipFumoOne(ps, resp); }
                    catch (Throwable t) { log.warn("emit fumo one err: {}", t.toString()); }
                })
                .onErrorResume(ex -> {
                    log.warn("fumo.one ERROR rid={}, equipType={}, ex={}", rid, equipType, ex.toString());
                    return Mono.empty();
                })
                .then();
    }

    /** op=13: add exp; param1=equipType, param2=addExp */
    private Mono<Void> onFumoAddExp(PlayerSession ps, Msgequip.PB_CSEquipReq req) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();
        final int equipType = req.hasParam1() ? req.getParam1() : 0;
        final int addExp    = req.hasParam2() ? req.getParam2() : 0;
        if (equipType <= 0 || addExp <= 0) return Mono.empty();

        var body = new EquipFumoDTOs.AddExpReq(rid, equipType, addExp, /*costItems*/ java.util.Map.of());

        return FeignCall.withToken(tk, "fumo.add-exp", () -> equipFumoFeign.addExp(body))
                // Sau khi addExp, luôn lấy lại ONE để client sync UI — không cần ResetOk/OK-ACK
                .onErrorResume(ex -> {
                    log.warn("fumo.add-exp ERROR rid={}, equipType={}, addExp={}, ex={}",
                            rid, equipType, addExp, ex.toString());
                    return Mono.empty();
                })
                .then(Mono.defer(() ->
                        FeignCall.withToken(tk, "fumo.one", () -> equipFumoFeign.one(rid, equipType))
                                .doOnNext(resp -> {
                                    try { Emitters.sendEquipFumoOne(ps, resp); }
                                    catch (Throwable t) { log.warn("emit fumo one after add-exp err: {}", t.toString()); }
                                })
                                .onErrorResume(ex -> Mono.empty())
                ))
                .then();
    }

    /** op=14: activate; param1=equipType, param2=endTimeEpochSec */
    private Mono<Void> onFumoActivate(PlayerSession ps, Msgequip.PB_CSEquipReq req) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();
        final int equipType = req.hasParam1() ? req.getParam1() : 0;
        final int endTime   = req.hasParam2() ? req.getParam2() : 0;
        if (equipType <= 0) return Mono.empty();

        var body = new EquipFumoDTOs.ActivateReq(rid, equipType, endTime);

        return FeignCall.withToken(tk, "fumo.activate", () -> equipFumoFeign.activate(body))
                .onErrorResume(ex -> {
                    log.warn("fumo.activate ERROR rid={}, equipType={}, endTime={}, ex={}",
                            rid, equipType, endTime, ex.toString());
                    return Mono.empty();
                })
                .then(Mono.defer(() ->
                        FeignCall.withToken(tk, "fumo.one", () -> equipFumoFeign.one(rid, equipType))
                                .doOnNext(resp -> {
                                    try { Emitters.sendEquipFumoOne(ps, resp); }
                                    catch (Throwable t) { log.warn("emit fumo one after activate err: {}", t.toString()); }
                                })
                                .onErrorResume(ex -> Mono.empty())
                ))
                .then();
    }

    /** op=15: reset; param1=equipType — KHÔNG dùng ResetOk; gửi lại ONE sau khi reset */
    private Mono<Void> onFumoReset(PlayerSession ps, Msgequip.PB_CSEquipReq req) {
        final String tk  = ps.getSessionId();
        final String rid = ps.getRoleId();
        final int equipType = req.hasParam1() ? req.getParam1() : 0;
        if (equipType <= 0) return Mono.empty();

        var body = new EquipFumoDTOs.ResetReq(rid, equipType, /*costItems*/ java.util.Map.of());

        return FeignCall.withToken(tk, "fumo.reset", () -> equipFumoFeign.reset(body))
                .onErrorResume(ex -> {
                    log.warn("fumo.reset ERROR rid={}, equipType={}, ex={}", rid, equipType, ex.toString());
                    return Mono.empty();
                })
                .then(Mono.defer(() ->
                        FeignCall.withToken(tk, "fumo.one", () -> equipFumoFeign.one(rid, equipType))
                                .doOnNext(resp -> {
                                    try { Emitters.sendEquipFumoOne(ps, resp); }
                                    catch (Throwable t) { log.warn("emit fumo one after reset err: {}", t.toString()); }
                                })
                                .onErrorResume(ex -> Mono.empty())
                ))
                .then();
    }
}