package com.SouthMillion.webSocket_server.handler.role;

import com.google.protobuf.ByteString;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.*;
import com.SouthMillion.webSocket_server.service.InMemoryPlayerSessionRegistry;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import com.SouthMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.SouthMillion.proto.Msgother.Msgother;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
@RequiredArgsConstructor
public class RoleServiceHandler implements MessageHandler {

    private final RoleFeign roleFeign;
    private final InMemoryPlayerSessionRegistry registry;

    @Override
    public int[] interests() {
        return new int[]{
                MsgIds.CS_ROLE_WXINFO_SET,
                MsgIds.CS_ROLE_SYSTEM_SET_REQ,
                MsgIds.CS_GET_OTHER_ROLE_INFO,
                MsgIds.CS_NOTICE_TIME_REQ
        };
    }

    /** Gọi sau login: đẩy role attributes/stats (1401) về client. */
    public Mono<Void> pushAll(PlayerSession ps) {
        if (blank(ps.getUserId()) || ps.getRoleId() == null) return Mono.empty();
        return FeignCall.withToken(ps.getSessionId(), "role-attr.self",
                        () -> roleFeign.getOtherRole(ps.getUserId(), String.valueOf(ps.getRoleId())))
                .doOnNext(info -> emitAttrListFromOtherRoleInfo(ps, info, 0))
                .onErrorResume(e -> {
                    log.warn("[role-attr] pushAll error: {}", e.toString());
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Push full role state after an external mutation (e.g. box sell gives exp).
     *
     * Emits:
     *  - 1400 PB_SCRoleInfoAck (authoritative snapshot)
     *  - 1402 PB_SCRoleExpChange (snapshot-style with change_exp=0)
     *  - 1403 PB_SCRoleLevelChange (snapshot-style)
     *  - 1401 PB_SCRoleAttrList (via pushAll)
     */
    public Mono<Void> pushRoleState(PlayerSession ps) {
        if (blank(ps.getUserId()) || ps.getRoleId() == null) return Mono.empty();

        Mono<Void> baseInfo = FeignCall.withToken(ps.getSessionId(), "role.by-user.snapshot",
                        () -> roleFeign.listByUser(ps.getUserId()))
                .doOnNext(list -> {
                    RoleDTOs.RoleResp role = selectCurrentRole(list, ps.getRoleId());
                    if (role == null) return;

                    Emitters.sendRoleInfoAck(ps, role);

                    long curExp = role.getCurExp() != null ? role.getCurExp() : 0L;
                    int level = role.getLevel() != null ? role.getLevel() : 1;

                    Msgrole.PB_SCRoleExpChange exp = Msgrole.PB_SCRoleExpChange.newBuilder()
                            .setChangeExp(0L)
                            .setCurExp(curExp)
                            .build();
                    Emitters.emit(ps, MsgIds.SC_ROLE_EXP_CHANGE, exp);

                    Msgrole.PB_SCRoleLevelChange lvl = Msgrole.PB_SCRoleLevelChange.newBuilder()
                            .setLevel(level)
                            .setExp(curExp)
                            .build();
                    Emitters.emit(ps, MsgIds.SC_ROLE_LEVEL_CHANGE, lvl);
                })
                .onErrorResume(e -> {
                    log.warn("[role-state] snapshot push failed: {}", e.toString());
                    return Mono.empty();
                })
                .then();

        return baseInfo.then(pushAll(ps));
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        try {
            return switch (msgId) {
                case MsgIds.CS_ROLE_WXINFO_SET      -> onWxInfoSet(ps, payload);
                case MsgIds.CS_ROLE_SYSTEM_SET_REQ  -> onSystemSet(ps, payload);
                case MsgIds.CS_GET_OTHER_ROLE_INFO  -> onGetOtherRole(ps, payload);
                case MsgIds.CS_NOTICE_TIME_REQ      -> onNoticeTimeReq(ps, payload);
                default -> Mono.empty();
            };
        } catch (Throwable t) {
            log.warn("[role-handler] error: {}", t.toString());
            return Mono.empty();
        }
    }

    /* ======================= WX INFO ======================= */
    private Mono<Void> onWxInfoSet(PlayerSession ps, byte[] payload) {
        if (blank(ps.getUserId())) return Mono.empty();

        final Msgrole.PB_CSRoleWXInfoSetReq req = parse(payload, Msgrole.PB_CSRoleWXInfoSetReq::parseFrom);
        if (req == null) return Mono.empty();

        String name = req.hasName() ? req.getName().toString(StandardCharsets.UTF_8) : null;
        String head = req.hasHeadChar() ? req.getHeadChar().toString(StandardCharsets.UTF_8) : null;

        var body = new RoleDTOs.WxInfoSetReq(name, head);

        // Lấy before để so sánh exp/level (phục vụ 1402/1403)
        Mono<List<RoleDTOs.RoleResp>> beforeMono = FeignCall
                .withToken(ps.getSessionId(), "role.getById", () -> roleFeign.listByUser(ps.getUserId()))
                .defaultIfEmpty(null);

        return beforeMono.flatMap(before ->
                FeignCall.withToken(ps.getSessionId(), "role.wxinfo",
                                () -> roleFeign.setWxInfo(ps.getRoleId(), body))
                        .doOnNext(after -> {
                            // bind lại & emit RoleInfo
                            registry.bindRoleToSession(ps, after.getRoleId() != null ? Long.parseLong(after.getRoleId()) : null, after.getUserId(), after.getName());
                            Emitters.emit(ps, MsgIds.SC_ROLE_INFO_ACK, buildRoleInfoAck(after));
                            // emit thay đổi EXP/LEVEL nếu có
                            emitExpChangeIfAny(ps, (RoleDTOs.RoleResp) before, after);
                            emitLevelChangeIfAny(ps, (RoleDTOs.RoleResp) before, after);
                        })
                        .then()
        );
    }

    /* ======================= SETTINGS ======================= */
    private Mono<Void> onSystemSet(PlayerSession ps, byte[] payload) {
        if (blank(ps.getUserId())) return Mono.empty();
        final Msgrole.PB_CSRoleSystemSetReq req = parse(payload, Msgrole.PB_CSRoleSystemSetReq::parseFrom);
        if (req == null) return Mono.empty();

        List<SettingsDTOs.SystemSettingItem> items = req.getSystemSetListList().stream()
                .map(x -> new SettingsDTOs.SystemSettingItem(String.valueOf(x.getSystemSetType()), x.getSystemSetParam()))
                .toList();
        var body = new SettingsDTOs.SystemSetReq(ps.getUserId(), items);

        return FeignCall.withToken(ps.getSessionId(), "role.settings",
                        () -> roleFeign.applySettings(body))
                .doOnNext(resp -> {
                    // Echo 1461
                    Msgrole.PB_SCRoleSystemSetInfo.Builder b = Msgrole.PB_SCRoleSystemSetInfo.newBuilder();
                    for (var it : items) {
                        int type = safeInt(it.key());
                        int param = (it.value() instanceof Number n) ? n.intValue() : safeInt(String.valueOf(it.value()));
                        b.addSystemSetList(Msgrole.PB_system_set.newBuilder()
                                .setSystemSetType(type)
                                .setSystemSetParam(param));
                    }
                    Emitters.emit(ps, MsgIds.SC_ROLE_SYSTEM_SET_INFO, b.build());
                })
                // Lấy attr & capability mới → phát 1401 (notify_reason: CHANGE=1)
                .then(FeignCall.withToken(ps.getSessionId(), "other-role.self",
                        () -> roleFeign.getOtherRole(ps.getUserId(), ps.getRoleId() != null ? String.valueOf(ps.getRoleId()) : null)))
                .doOnNext(info -> emitAttrListFromOtherRoleInfo(ps, info, /*notifyReason=*/1))
                .then();
    }

    /* ======================= OTHER ROLE ======================= */
    private Mono<Void> onGetOtherRole(PlayerSession ps, byte[] payload) {
        if (blank(ps.getUserId())) return Mono.empty();
        final Msgrole.PB_CSGetOtherRoleInfo req = parse(payload, Msgrole.PB_CSGetOtherRoleInfo::parseFrom);
        if (req == null) return Mono.empty();

        int uid = req.hasUid() ? req.getUid() : 0;

        return FeignCall.withToken(ps.getSessionId(), "other-role.get",
                        () -> roleFeign.getOtherRole(Integer.toString(uid), null))
                .doOnNext(info -> {
                    // Tối thiểu trả uid (bạn có thể mở rộng map đầy đủ)
                    Msgrole.PB_SCGetOtherRoleRet out = Msgrole.PB_SCGetOtherRoleRet.newBuilder()
                            .setUid(uid)
                            .build();
                    Emitters.emit(ps, MsgIds.SC_GET_OTHER_ROLE_RET, out);
                })
                .then();
    }

    /* ======================= NOTICE TIME ======================= */

    /**
     * CS:1464 PB_CSNoticeTimeReq — client hỏi thời điểm hiển thị notice tiếp theo.
     * SC:1465 PB_SCNoticeTimeRet — trả về danh sách notice_id + thời gian (epoch giây).
     * Nếu role-service không cung cấp endpoint, trả về list rỗng để client không bị block.
     */
    private Mono<Void> onNoticeTimeReq(PlayerSession ps, byte[] payload) {
        if (ps.getRoleId() == null) return Mono.empty();
        final Msgother.PB_CSNoticeTimeReq req = parse(payload, Msgother.PB_CSNoticeTimeReq::parseFrom);
        int type = req != null && req.hasType() ? req.getType() : 0;
        long param = req != null && req.hasParam() ? req.getParam() : 0L;

        Map<String, Object> body = Map.of("type", type, "param", param);

        return FeignCall.withToken(ps.getSessionId(), "role.noticeTime",
                        () -> roleFeign.noticeTime(String.valueOf(ps.getRoleId()), body))
                .doOnNext(resp -> {
                    long noticeTime = 0L;
                    if (resp != null && resp.get("noticeTime") instanceof Number n) {
                        noticeTime = n.longValue();
                    }
                    Msgother.PB_SCNoticeTimeRet out = Msgother.PB_SCNoticeTimeRet.newBuilder()
                            .setNoticeTime(noticeTime)
                            .build();
                    Emitters.emit(ps, MsgIds.SC_NOTICE_TIME_RET, out);
                })
                .onErrorResume(e -> {
                    log.warn("[notice-time] role-service call failed: {}", e.toString());
                    Msgother.PB_SCNoticeTimeRet out = Msgother.PB_SCNoticeTimeRet.newBuilder()
                            .setNoticeTime(0L)
                            .build();
                    Emitters.emit(ps, MsgIds.SC_NOTICE_TIME_RET, out);
                    return Mono.empty();
                })
                .then();
    }

    /* ======================= EMIT 1401 / 1402 / 1403 ======================= */

    /**
     * Lấy từ DTO OtherRoleInfo (bằng reflection) → build PB_SCRoleAttrList rồi phát 1401.
     * @param notifyReason 0=INIT, 1=CHANGE (theo mẫu C++).
     */
    private void emitAttrListFromOtherRoleInfo(PlayerSession ps, OtherRoleDTOs.OtherRoleInfo info, int notifyReason) {
        Msgrole.PB_SCRoleAttrList.Builder b = Msgrole.PB_SCRoleAttrList.newBuilder();

        // notify_reason
        try { b.setNotifyReason(notifyReason); } catch (Throwable ignore) {}

        // capability: try explicit field first, fallback from core attrs if needed
        long cap = 0L;
        try {
            Object v = callGetterFirst(info, "getCapability", "getCap", "capability", "cap");
            if (v instanceof Number n) cap = n.longValue();
        } catch (Throwable ignore) {}

        // attr list: support both collection style and record style attributes object
        try {
            Object rawList = callGetterFirst(
                    info,
                    "getRoleAttrList", "getAttrList", "getAttrs",
                    "roleAttrList", "attrList", "attrs", "attributes"
            );
            if (rawList instanceof Collection<?> col) {
                for (Object elt : col) {
                    Integer t = asInt(callGetterFirst(elt, "getAttrType", "getType", "attrType", "type"));
                    Long    v = asLong(callGetterFirst(elt, "getAttrValue", "getValue", "attrValue", "value"));
                    if (t == null || v == null) continue;
                    addAttrPair(b, t, v);
                }
            } else if (rawList != null) {
                // OtherRoleInfo.attributes() is a single object with core fields.
                Long hp = asLong(callGetterFirst(rawList, "getHp", "hp"));
                Long atk = asLong(callGetterFirst(rawList, "getAttackValue", "attackValue", "getAtk", "atk"));
                Long def = asLong(callGetterFirst(rawList, "getDefenseValue", "defenseValue", "getDef", "def"));
                Long spd = asLong(callGetterFirst(rawList, "getSpeed", "speed"));

                if (hp != null) addAttrPair(b, 1, hp);
                if (atk != null) addAttrPair(b, 2, atk);
                if (def != null) addAttrPair(b, 3, def);
                if (spd != null) addAttrPair(b, 4, spd);

                if (cap <= 0) {
                    cap = (hp != null ? hp : 0L)
                            + (atk != null ? atk : 0L)
                            + (def != null ? def : 0L)
                            + (spd != null ? spd * 10L : 0L);
                }
            }
        } catch (Throwable e) {
            log.debug("[attr-list] reflection mapping skipped: {}", e.toString());
        }

        try { b.setCapability(cap); } catch (Throwable ignore) {}

        Emitters.emit(ps, MsgIds.SC_ROLE_ATTR_LIST, b.build());
    }

    private void addAttrPair(Msgrole.PB_SCRoleAttrList.Builder b, int attrType, long attrValue) {
        try {
            Msgrole.PB_AttrPair pair = Msgrole.PB_AttrPair.newBuilder()
                    .setAttrType(attrType)
                    .setAttrValue(attrValue)
                    .build();
            b.addAttrList(pair);
        } catch (Throwable ignore) {}
    }

    /** EXP thay đổi → phát 1402 (change_exp, cur_exp). */
    private void emitExpChangeIfAny(PlayerSession ps, RoleDTOs.RoleResp before, RoleDTOs.RoleResp after) {
        long be = (before != null && before.getCurExp() != null) ? before.getCurExp() : -1L;
        long af = (after  != null && after.getCurExp()  != null) ? after.getCurExp()  : -1L;
        if (be == -1L || af == -1L || be == af) return;

        long delta = af - be;

        Msgrole.PB_SCRoleExpChange out = Msgrole.PB_SCRoleExpChange.newBuilder()
                .setChangeExp(delta)
                .setCurExp(af)
                .build();

        Emitters.emit(ps, MsgIds.SC_ROLE_EXP_CHANGE, out);
    }

    /** LEVEL thay đổi → phát 1403 (level, exp). */
    private void emitLevelChangeIfAny(PlayerSession ps, RoleDTOs.RoleResp before, RoleDTOs.RoleResp after) {
        Integer bl = (before != null) ? before.getLevel() : null;
        Integer al = (after  != null) ? after.getLevel()  : null;
        if (bl == null || al == null || Objects.equals(bl, al)) return;

        long curExp = (after.getCurExp() != null) ? after.getCurExp() : 0L;

        Msgrole.PB_SCRoleLevelChange out = Msgrole.PB_SCRoleLevelChange.newBuilder()
                .setLevel(al)
                .setExp(curExp)
                .build();

        Emitters.emit(ps, MsgIds.SC_ROLE_LEVEL_CHANGE, out);
    }

    /* ======================= helpers ======================= */

    private Msgrole.PB_SCRoleInfoAck buildRoleInfoAck(RoleDTOs.RoleResp role) {
        int roleId = safeInt(role.getRoleId());
        String name = role.getName() != null ? role.getName() : "Player";
        int level = role.getLevel() != null ? role.getLevel() : 1;
        long curExp = role.getCurExp() != null ? role.getCurExp() : 0L;
        String headChar = role.getHeadChar() != null ? role.getHeadChar() : "";

        Msgrole.PB_RoleInfo.Builder base = Msgrole.PB_RoleInfo.newBuilder()
                .setRoleId(roleId)
                .setName(ByteString.copyFrom(name.getBytes(StandardCharsets.UTF_8)))
                .setLevel(level)
                .setCap(0L) // nếu cần: map từ DTO
                .setHeadPicId(0)
                .setTitleId(0)
                .setGuildName(ByteString.EMPTY)
                .setKnightLevel(0)
                .setHeadChar(ByteString.copyFrom(headChar.getBytes(StandardCharsets.UTF_8)));

        return Msgrole.PB_SCRoleInfoAck.newBuilder()
                .setCurExp(curExp)
                .setCreateTime(0)
                .setRoleinfo(base)
                .setAppearance(Msgrole.PB_Appearance.newBuilder().build())
                .build();
    }


    private boolean blank(String s) { return s == null || s.isBlank(); }

    private int safeInt(String s) {
        if (s == null) return 0;
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    @FunctionalInterface
    private interface Parser<T> { T parse(byte[] b) throws Exception; }

    private <T> T parse(byte[] bytes, Parser<T> p) {
        try { return p.parse(bytes); } catch (Exception e) {
            log.warn("[parse] {}", e.toString()); return null;
        }
    }

    /* ===== reflection utils cho mapping DTO linh hoạt ===== */
    private static Object callGetter(Object obj, String method) throws Exception {
        if (obj == null) return null;
        Method m = obj.getClass().getMethod(method);
        return m.invoke(obj);
    }

    private static Object callGetterFirst(Object obj, String... names) throws Exception {
        if (obj == null) return null;
        for (String n : names) {
            try {
                Method m = obj.getClass().getMethod(n);
                return m.invoke(obj);
            } catch (NoSuchMethodException ignore) {}
        }
        return null;
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private RoleDTOs.RoleResp selectCurrentRole(List<RoleDTOs.RoleResp> list, Long roleId) {
        if (list == null || list.isEmpty()) return null;
        String wanted = String.valueOf(roleId);
        for (RoleDTOs.RoleResp role : list) {
            if (role != null && Objects.equals(role.getRoleId(), wanted)) {
                return role;
            }
        }
        return list.get(0);
    }
}