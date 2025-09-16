package com.southMillion.webSocket_server.handler.role;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.*;
import com.southMillion.webSocket_server.service.InMemoryPlayerSessionRegistry;
import com.southMillion.webSocket_server.service.client.RoleFeign;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.role.advertisment.AdvertisementDTOs;
import org.SouthMillion.dto.role.mail.MailDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.SouthMillion.proto.Msgother.Msgother;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.SouthMillion.proto.msgmail.Msgmail;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
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
                MsgIds.CS_MAIL_REQ,
                MsgIds.CS_ADVERTISEMENT_FETCH,
                MsgIds.CS_GET_OTHER_ROLE_INFO
        };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        try {
            return switch (msgId) {
                case MsgIds.CS_ROLE_WXINFO_SET      -> onWxInfoSet(ps, payload);
                case MsgIds.CS_ROLE_SYSTEM_SET_REQ  -> onSystemSet(ps, payload);
                case MsgIds.CS_MAIL_REQ             -> onMail(ps, payload);
                case MsgIds.CS_ADVERTISEMENT_FETCH  -> onAdFetch(ps, payload);
                case MsgIds.CS_GET_OTHER_ROLE_INFO  -> onGetOtherRole(ps, payload);
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
                .withToken(ps.getSessionId(), "role.getById", () -> roleFeign.listByUser(ps.getRoleId()))
                .defaultIfEmpty(null);

        return beforeMono.flatMap(before ->
                FeignCall.withToken(ps.getSessionId(), "role.wxinfo",
                                () -> roleFeign.setWxInfo(ps.getRoleId(), body))
                        .doOnNext(after -> {
                            // bind lại & emit RoleInfo
                            registry.bindRoleToSession(ps, after.getRoleId(), after.getUserId(), after.getName());
                            send(ps, MsgIds.SC_ROLE_INFO_ACK, buildRoleInfoAck(after));
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
                    send(ps, MsgIds.SC_ROLE_SYSTEM_SET_INFO, b.build());
                })
                // Lấy attr & capability mới → phát 1401 (notify_reason: CHANGE=1)
                .then(FeignCall.withToken(ps.getSessionId(), "other-role.self",
                        () -> roleFeign.getOtherRole(ps.getUserId(), ps.getRoleId())))
                .doOnNext(info -> emitAttrListFromOtherRoleInfo(ps, info, /*notifyReason=*/1))
                .then();
    }

    /* ======================= MAIL ======================= */
    private Mono<Void> onMail(PlayerSession ps, byte[] payload) {
        if (blank(ps.getUserId())) return Mono.empty();
        final Msgmail.PB_CSMailReq req = parse(payload, Msgmail.PB_CSMailReq::parseFrom);
        if (req == null) return Mono.empty();

        int type = req.hasType() ? req.getType() : 0;
        String userId = ps.getUserId();
        String mailId = req.hasP1() ? Integer.toString(req.getP1()) : null;

        return switch (type) {
            case 0 -> FeignCall.withToken(ps.getSessionId(), "mail.list",
                            () -> roleFeign.mailList(new MailDTOs.MailListReq(userId)))
                    .doOnNext(resp -> send(ps, MsgIds.SC_MAIL_LIST_ACK, Msgmail.PB_SCMailListAck.newBuilder().build()))
                    .then();

            case 1 -> FeignCall.withToken(ps.getSessionId(), "mail.detail",
                            () -> roleFeign.mailDetail(userId, mailId))
                    .doOnNext(resp -> send(ps, MsgIds.SC_MAIL_DETAIL, Msgmail.PB_SCMailDetail.newBuilder().build()))
                    .then();

            case 2 -> FeignCall.withToken(ps.getSessionId(), "mail.delete",
                            () -> roleFeign.mailDelete(userId, mailId))
                    .doOnNext(resp -> send(ps, MsgIds.SC_MAIL_DELETE_ACK, Msgmail.PB_SCMailDeleteAck.newBuilder().build()))
                    .then();

            case 3 -> FeignCall.withToken(ps.getSessionId(), "mail.fetch",
                            () -> roleFeign.mailFetch(userId, mailId))
                    .doOnNext(resp -> send(ps, MsgIds.SC_FETCH_MAIL_ACK, Msgmail.PB_SCFetchMailAck.newBuilder().build()))
                    .then();

            default -> {
                log.warn("[mail] unknown type={}", type);
                yield Mono.empty();
            }
        };
    }

    /* ======================= ADS ======================= */
    private Mono<Void> onAdFetch(PlayerSession ps, byte[] payload) {
        if (blank(ps.getUserId())) return Mono.empty();
        final Msgother.PB_CSAdvertisementFetch req = parse(payload, Msgother.PB_CSAdvertisementFetch::parseFrom);
        if (req == null) return Mono.empty();

        var body = new AdvertisementDTOs.AdFetchReq(
                ps.getUserId(), ps.getRoleId(),
                req.hasSeq() ? req.getSeq() : 0,
                req.hasIsDia() ? req.getIsDia() : 0,
                req.hasParam() ? req.getParam() : 0
        );

        return FeignCall.withToken(ps.getSessionId(), "ads.claim",
                        () -> roleFeign.claimAd(body))
                .doOnNext(info -> {
                    Msgother.PB_SCAdvertisement one = Msgother.PB_SCAdvertisement.newBuilder()
                            .setSeq(body.seq())
                            .setTodayCount(0)
                            .setNextFetchTime(0)
                            .build();
                    Msgother.PB_SCAdvertisementInfo out = Msgother.PB_SCAdvertisementInfo.newBuilder()
                            .addAdList(one)
                            .setIsInit(0)
                            .build();
                    send(ps, MsgIds.SC_ADVERTISEMENT_INFO, out);
                })
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
                    send(ps, MsgIds.SC_GET_OTHER_ROLE_RET, out);
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

        // capability: tìm getCapability() hoặc getCap()
        long cap = 0L;
        try {
            Object v = callGetter(info, "getCapability");
            if (v == null) v = callGetter(info, "getCap");
            if (v instanceof Number n) cap = n.longValue();
        } catch (Throwable ignore) {}
        try { b.setCapability(cap); } catch (Throwable ignore) {}

        // attr list: getRoleAttrList() | getAttrList() | getAttrs() → collection
        try {
            Object rawList = callGetterFirst(info, "getRoleAttrList", "getAttrList", "getAttrs");
            if (rawList instanceof Collection<?> col) {
                for (Object elt : col) {
                    Integer t = asInt(callGetterFirst(elt, "getAttrType", "getType", "attrType", "type"));
                    Long    v = asLong(callGetterFirst(elt, "getAttrValue", "getValue", "attrValue", "value"));
                    if (t == null || v == null) continue;
                    Msgrole.PB_AttrPair pair = Msgrole.PB_AttrPair.newBuilder()
                            .setAttrType(t)
                            .setAttrValue(v)
                            .build();
                    b.addAttrList(pair);
                }
            }
        } catch (Throwable e) {
            log.debug("[attr-list] reflection mapping skipped: {}", e.toString());
        }

        send(ps, MsgIds.SC_ROLE_ATTR_LIST, b.build());
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

        send(ps, MsgIds.SC_ROLE_EXP_CHANGE, out);
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

        send(ps, MsgIds.SC_ROLE_LEVEL_CHANGE, out);
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

    private void send(PlayerSession ps, int msgId, MessageLite pb) {
        byte[] payload = pb != null ? pb.toByteArray() : new byte[0];
        byte[] frame = PacketCodec.encode(msgId, payload);
        ps.sendBinary(frame);
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
}