package com.SouthMillion.webSocket_server.handler.role;

import com.google.protobuf.ByteString;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.task.TaskHandler;
import com.SouthMillion.webSocket_server.net.*;
import com.SouthMillion.webSocket_server.service.InMemoryPlayerSessionRegistry;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.MountFeign;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import com.SouthMillion.webSocket_server.service.client.ShiZhuangFeign;
import com.SouthMillion.webSocket_server.service.grpc.AngelGrpcClient;
import com.SouthMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.SouthMillion.dto.role.settings.SystemSettings;
import org.SouthMillion.proto.Msgother.Msgother;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoleServiceHandler.class);

    private final RoleFeign roleFeign;
    private final ShiZhuangFeign shiZhuangFeign;
    private final MountFeign mountFeign;
    private final AngelGrpcClient angelGrpcClient;
    private final InMemoryPlayerSessionRegistry registry;
    private final TaskProgressPublisher taskProgressPublisher;

    /** Setter injection with @Lazy to break the potential construction-order coupling. */
    @Setter(onMethod = @__({@Autowired, @Lazy}))
    private TaskHandler taskHandler;

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
                .doOnNext(info -> {
                    // Khởi tạo session cache để emitLevelChangeFromSessionCacheIfAny
                    // hoạt động đúng ngay từ action đầu tiên sau login.
                    if (info != null && info.attributes() != null) {
                        ps.setLastKnownRoleLevel(info.attributes().level());
                        ps.setLastKnownRoleExp(info.attributes().exp());
                    }
                    emitAttrListFromOtherRoleInfo(ps, info, 0);
                })
                .onErrorResume(e -> {
                    log.warn("[role-attr] pushAll error: {}", e.toString());
                    return Mono.empty();
                })
                .then(pushSystemSettings(ps));
    }

    /**
     * Push full role state after an external mutation (e.g. box sell gives exp).
     *
     * Emits:
     *  - 1400 PB_SCRoleInfoAck (authoritative snapshot)
     *  - 1402 PB_SCRoleExpChange (snapshot-style with change_exp=0)
     *  - 1401 PB_SCRoleAttrList (via pushAll)
    *
    * NOTE:
    *  - This method must NOT emit 1403 (PB_SCRoleLevelChange).
    *  - 1403 is reserved for true level delta notifications only
    *    (see emitLevelChangeIfAny) so client levelup popup semantics remain correct.
     */
    public Mono<Void> pushRoleState(PlayerSession ps) {
        if (blank(ps.getUserId()) || ps.getRoleId() == null) return Mono.empty();

        Mono<Void> baseInfo = FeignCall.withToken(ps.getSessionId(), "role.by-user.snapshot",
                        () -> roleFeign.listByUser(ps.getUserId()))
                .doOnNext(list -> {
                    RoleDTOs.RoleResp role = selectCurrentRole(list, ps.getRoleId());
                    if (role == null) return;

                    emitLevelChangeFromSessionCacheIfAny(ps, role);
                    emitRoleInfoAck(ps, role);

                    long curExp = role.getCurExp() != null ? role.getCurExp() : 0L;
                    Msgrole.PB_SCRoleExpChange exp = Msgrole.PB_SCRoleExpChange.newBuilder()
                            .setChangeExp(0L)
                            .setCurExp(curExp)
                            .build();
                    Emitters.emit(ps, MsgIds.SC_ROLE_EXP_CHANGE, exp);
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
                            emitRoleInfoAck(ps, after);
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
                .map(this::toRoleSettingItem)
                .filter(Objects::nonNull)
                .toList();
        var body = new SettingsDTOs.SystemSetReq(ps.getUserId(), items);

        return FeignCall.withToken(ps.getSessionId(), "role.settings",
                        () -> roleFeign.applySettings(body))
                .doOnNext(resp -> emitSystemSettings(ps, resp))
                // Lấy attr & capability mới → phát 1401 (notify_reason: CHANGE=1)
                .then(FeignCall.withToken(ps.getSessionId(), "other-role.self",
                        () -> roleFeign.getOtherRole(ps.getUserId(), ps.getRoleId() != null ? String.valueOf(ps.getRoleId()) : null)))
                .doOnNext(info -> emitAttrListFromOtherRoleInfo(ps, info, /*notifyReason=*/1))
                .then();
    }

    private Mono<Void> pushSystemSettings(PlayerSession ps) {
        if (blank(ps.getUserId())) {
            return Mono.empty();
        }
        SettingsDTOs.SystemSetReq body = new SettingsDTOs.SystemSetReq(ps.getUserId(), List.of());
        return FeignCall.withToken(ps.getSessionId(), "role.settings.fetch",
                        () -> roleFeign.applySettings(body))
                .doOnNext(resp -> emitSystemSettings(ps, resp))
                .onErrorResume(e -> {
                    log.warn("[role-settings] pushAll error: {}", e.toString());
                    return Mono.empty();
                })
                .then();
    }

    private void emitSystemSettings(PlayerSession ps, SettingsDTOs.SystemSetResp resp) {
        SystemSettings settings = resp != null && resp.settings() != null ? resp.settings() : SystemSettings.defaults();

        Msgrole.PB_SCRoleSystemSetInfo.Builder b = Msgrole.PB_SCRoleSystemSetInfo.newBuilder();
        b.addSystemSetList(Msgrole.PB_system_set.newBuilder()
                .setSystemSetType(0)
                .setSystemSetParam(toLegacyToggleParam(settings.getMusicOn())));
        b.addSystemSetList(Msgrole.PB_system_set.newBuilder()
                .setSystemSetType(1)
                .setSystemSetParam(toLegacyToggleParam(settings.getSfxOn())));
        b.addSystemSetList(Msgrole.PB_system_set.newBuilder()
                .setSystemSetType(2)
                .setSystemSetParam(toLegacyToggleParam(settings.getVibrateOn())));
        Emitters.emit(ps, MsgIds.SC_ROLE_SYSTEM_SET_INFO, b.build());
    }

    private SettingsDTOs.SystemSettingItem toRoleSettingItem(Msgrole.PB_system_set item) {
        if (item == null) {
            return null;
        }
        int type = item.getSystemSetType();
        int param = item.getSystemSetParam();
        return switch (type) {
            case 0 -> new SettingsDTOs.SystemSettingItem("music", param == 0);
            case 1 -> new SettingsDTOs.SystemSettingItem("sfx", param == 0);
            case 2 -> new SettingsDTOs.SystemSettingItem("vibrate", param == 0);
            default -> new SettingsDTOs.SystemSettingItem(String.valueOf(type), param);
        };
    }

    private int toLegacyToggleParam(Boolean enabled) {
        return Boolean.FALSE.equals(enabled) ? 1 : 0;
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

    /** LEVEL thay đổi → phát 1403 (level, exp) + cập nhật task level_up. */
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

        // Report level_up task progress and push 1452 with animation for each level gained.
        // taskHandler may be null during early startup (lazy-init); guard accordingly.
        if (taskHandler != null) {
            try {
                taskHandler.pushLevelUpProgress(ps, bl, al);
            } catch (Exception e) {
                log.debug("[role] pushLevelUpProgress failed roleId={} ex={}", ps.getRoleId(), e.toString());
            }
        } else {
            try {
                taskProgressPublisher.publish(ps.getRoleId(), "level_up", al, "role-level-change");
            } catch (Exception e) {
                log.debug("[role] taskProgressPublisher level_up failed roleId={} ex={}", ps.getRoleId(), e.toString());
            }
        }
    }

    /* ======================= helpers ======================= */

    public void emitRoleInfoAck(PlayerSession ps, RoleDTOs.RoleResp role) {
        if (ps == null || role == null) {
            return;
        }
        ps.setLastKnownRoleLevel(role.getLevel());
        ps.setLastKnownRoleExp(role.getCurExp() != null ? role.getCurExp() : 0L);
        Emitters.sendRoleInfoAck(ps, role, buildAppearance(role));
    }

    private void emitLevelChangeFromSessionCacheIfAny(PlayerSession ps, RoleDTOs.RoleResp after) {
        if (ps == null || after == null) {
            return;
        }
        Integer bl = ps.getLastKnownRoleLevel();
        Integer al = after.getLevel();
        if (bl == null || al == null || Objects.equals(bl, al)) {
            return;
        }

        long curExp = (after.getCurExp() != null) ? after.getCurExp() : 0L;
        Msgrole.PB_SCRoleLevelChange out = Msgrole.PB_SCRoleLevelChange.newBuilder()
                .setLevel(al)
                .setExp(curExp)
                .build();
        Emitters.emit(ps, MsgIds.SC_ROLE_LEVEL_CHANGE, out);

        if (taskHandler != null) {
            try {
                taskHandler.pushLevelUpProgress(ps, bl, al);
            } catch (Exception e) {
                log.debug("[role] pushLevelUpProgress cache-path failed roleId={} ex={}", ps.getRoleId(), e.toString());
            }
        } else {
            try {
                taskProgressPublisher.publish(ps.getRoleId(), "level_up", al, "role-level-change-cache");
            } catch (Exception e) {
                log.debug("[role] taskProgressPublisher level_up cache-path failed roleId={} ex={}", ps.getRoleId(), e.toString());
            }
        }
    }

    private Msgrole.PB_SCRoleInfoAck buildRoleInfoAck(RoleDTOs.RoleResp role) {
        Msgrole.PB_RoleInfo.Builder base = Msgrole.PB_RoleInfo.newBuilder()
                .setRoleId(parseInt(role.getRoleId(), 0))
                .setName(ByteString.copyFromUtf8(firstNonBlank(role.getName(), role.getNickname(), role.getRoleName(), "Player")))
                .setLevel(role.getLevel() != null ? role.getLevel() : 1)
                .setCap(parseLong(role.getCap(), 0L))
                .setHeadPicId(parseInt(role.getHeadPicId(), 0))
                .setTitleId(parseInt(role.getTitleId(), 0))
                .setGuildName(ByteString.copyFromUtf8(firstNonBlank(role.getGuildName())))
                .setKnightLevel(parseInt(role.getKnightLevel(), 0))
                .setHeadChar(ByteString.copyFromUtf8(firstNonBlank(role.getHeadChar())));

        return Msgrole.PB_SCRoleInfoAck.newBuilder()
                .setCurExp(parseLong(role.getCurExp(), 0L))
                .setCreateTime(parseLong(role.getCreateTimeEpochSec(), 0L))
                .setRoleinfo(base.build())
                .setAppearance(buildAppearance(role))
                .build();
    }

    private Msgrole.PB_Appearance buildAppearance(RoleDTOs.RoleResp role) {
        Msgrole.PB_Appearance.Builder appearance = Msgrole.PB_Appearance.newBuilder()
                .setSurfaceMount(-1)
                .setSurfaceAngel(-1);

        long roleId = parseLong(role != null ? role.getRoleId() : null, 0L);
        if (roleId <= 0L) {
            return appearance.build();
        }

        try {
            Map<String, Object> fashion = shiZhuangFeign.getCurrentAppearance(String.valueOf(roleId));
            applyAppearanceValue(appearance, fashion, "surfaceWeapon");
            applyAppearanceValue(appearance, fashion, "surfaceShield");
            applyAppearanceValue(appearance, fashion, "surfaceHead");
            applyAppearanceValue(appearance, fashion, "surfaceBody");
        } catch (Exception e) {
            log.debug("[role-appearance] shizhuang lookup skipped roleId={} ex={}", roleId, e.toString());
        }

        try {
            Map<String, Object> mountData = mountFeign.getMountData(String.valueOf(roleId));
            if (mountData != null && mountData.containsKey("appearanceId")) {
                appearance.setSurfaceMount(parseInt(mountData.get("appearanceId"), -1));
            }
        } catch (Exception e) {
            log.debug("[role-appearance] mount lookup skipped roleId={} ex={}", roleId, e.toString());
        }

        try {
            var angels = angelGrpcClient.getUserAngels(String.valueOf(roleId));
            appearance.setSurfaceAngel(resolveAngelAppearance(angels));
        } catch (Exception e) {
            log.debug("[role-appearance] angel lookup skipped roleId={} ex={}", roleId, e.toString());
        }

        return appearance.build();
    }

    private void applyAppearanceValue(Msgrole.PB_Appearance.Builder appearance, Map<String, Object> source, String key) {
        if (appearance == null || source == null || !source.containsKey(key)) {
            return;
        }
        int value = parseInt(source.get(key), 0);
        if (value <= 0) {
            // 0 nghĩa là không có trang phục/appearance — không set field để proto "has_*" = false
            // Client phân biệt: has=false → không hiện, has=true & value=0 → có thể hiện mặc định
            return;
        }
        switch (key) {
            case "surfaceWeapon" -> appearance.setSurfaceWeapon(value);
            case "surfaceShield" -> appearance.setSurfaceShield(value);
            case "surfaceHead" -> appearance.setSurfaceHead(value);
            case "surfaceBody" -> appearance.setSurfaceBody(value);
            case "surfaceMount" -> appearance.setSurfaceMount(value);
            case "surfaceAngel" -> appearance.setSurfaceAngel(value);
            default -> {
            }
        }
    }

    private int resolveAngelAppearance(org.SouthMillion.proto.angel.GetUserAngelsResponse response) {
        if (response == null || (response.hasStatus() && !response.getStatus().getSuccess()) || response.getAngelsCount() == 0) {
            return -1;
        }
        for (var angel : response.getAngelsList()) {
            if (angel.getIsEquipped()) {
                return angel.getAppearanceId();
            }
        }
        return -1;
    }

    private int parseInt(Object value, int def) {
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return def;
        }
    }

    private long parseLong(Object value, long def) {
        if (value == null) {
            return def;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return def;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
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