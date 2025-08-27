package com.southMillion.webSocket_server.handler.login;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.service.SessionRegistry;
import com.southMillion.webSocket_server.service.client.*;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.session.IntrospectResponse;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginBootstrapHandler implements MessageHandler {

    // ====== Feign / Registry ======
    private final SessionHttpClient  sessionFeign;
    private final RoleHttpClient     roleFeign;
    private final ConfigFeign        configFeign;

    private final BagPublicHttpClient  bagPublicFeign;
    private final BagInternalFeign     bagInternalFeign;
    private final EquipHttpClient      equipFeign;
    private final WalletHttpClient     walletFeign;
    private final BoxFeign             boxFeign;
    private final GiftFeign            giftFeign;
    private final ShopFeign            shopFeign;
    private final ItemMetaFeign        itemMetaFeign;

    private final SessionRegistry      registry;

    // ====== Runtime guard (server ready / busy) ======
    // Bạn có thể thay bằng bean riêng nếu muốn kiểm soát tập trung
    private static final Semaphore LOGIN_LIMITER = new Semaphore(128);
    private static volatile boolean WORLD_READY = true; // set false nếu server chưa load xong

    // ====== Constants ======
    private static final byte BAG_COMMON = 0;
    private static final byte BAG_EQUIP  = 1;

    private static final String REASON_FIRST_LOGIN_BOX = "FIRST_LOGIN_BOX";

    // CHANGED: srcMsgId/srcOp là int theo BagDTOs.AddItemReq
    private static final int SRC_MSG_LOGIN = 0;   // đặt 0 nếu chưa có mã cụ thể
    private static final int SRC_OP_GRANT  = 0;   // đặt 0 nếu chưa có mã cụ thể

    // Theo kinh nghiệm dự án trước: hộp khởi đầu thường là 40004
    private static final long DEFAULT_BOX_ITEM_ID = 40004L;

    // Mã result gợi ý (map với client của bạn)
    private static final int LOGIN_OK                 = 0;
    private static final int LOGIN_ERR_MISSING_TOKEN  = 2;
    private static final int LOGIN_ERR_PARSE          = 4;
    private static final int LOGIN_ERR_SERVER_NOTREADY= 11;
    private static final int LOGIN_ERR_SERVER_BUSY    = 12;
    private static final int LOGIN_ERR_FORBID         = 13;
    private static final int LOGIN_ERR_FORBID_NEWROLE = 14;

    private static final int DISCONNECT_REASON_LOGIN_OTHER_PLACE = 1;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public int[] interests() {
        return new int[]{ MsgIds.CS_LOGIN_REQ /*7056*/ };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        // 0) Server readiness / busy guard
        if (!WORLD_READY) {
            return sendLoginAck(ps, LOGIN_ERR_SERVER_NOTREADY, 0);
        }
        if (!LOGIN_LIMITER.tryAcquire()) {
            return sendLoginAck(ps, LOGIN_ERR_SERVER_BUSY, 0);
        }

        // 1) Parse yêu cầu
        final Msglogin.PB_CSLoginToAccount req;
        try {
            req = Msglogin.PB_CSLoginToAccount.parseFrom(payload);
        } catch (Exception e) {
            log.warn("[login] parse 7056 error: {}", e.toString());
            return sendLoginAck(ps, LOGIN_ERR_PARSE, 0)
                    .doFinally(s -> LOGIN_LIMITER.release());
        }

        final String token = (req != null && req.hasLoginStr()) ? req.getLoginStr() : null;
        if (!StringUtils.hasText(token)) {
            log.warn("[login] 7056 without login_str");
            return sendLoginAck(ps, LOGIN_ERR_MISSING_TOKEN, 0)
                    .doFinally(s -> LOGIN_LIMITER.release());
        }

        // 2) Thực xử lý
        return doHandle(ps, token)
                .doFinally(sig -> LOGIN_LIMITER.release());
    }

    private Mono<Void> doHandle(PlayerSession ps, String token) {
        final AtomicBoolean createdNow = new AtomicBoolean(false);

        return FeignCall.withToken(token, "session.introspect", () -> sessionFeign.introspect(token))
                .flatMap(ir -> {
                    // Forbid / ban?
                    long forbidRemainSec = calcForbidRemainSec(ir);
                    if (forbidRemainSec > 0) {
                         sendLoginAck(ps, LOGIN_ERR_FORBID, (int)forbidRemainSec);
                        return Mono.empty();
                    }

                    // Bind session theo user
                    ps.setLoggedIn(true);
                    ps.setSessionId(token);
                    ps.setUserId(ir.getUserId());
                    ps.setUsername(ir.getUsername());
                    registry.updateBindings(ps);

                    // Kick trùng đăng nhập (nếu có)
                    return kickOldSessionIfAny(ps)
                            .then( FeignCall.withToken(token, "role.list", () -> roleFeign.list(ps.getUserId())) );
                })
                // 3) Chọn role đầu tiên hoặc tạo mới
                .flatMap(roleList -> {
                    RoleDTOs.RoleResp rv = (roleList != null && roleList.getItems() != null && !roleList.getItems().isEmpty())
                            ? roleList.getItems().get(0) : null;

                    if (rv == null) {
                        // (Optional) Gate tạo mới theo config? (bỏ qua nếu chưa có)
                        createdNow.set(true);
                        return FeignCall.withToken(token, "role.create", () -> roleFeign.create(buildDefaultCreateRoleReq(ps)))
                                .map(newRole -> {
                                    ps.setRoleId(newRole.getRoleId());
                                    try { ps.setRoleLevel(safeInt(newRole.getLevel(), 1)); } catch (Throwable ignore) {}
                                    return newRole;
                                });
                    } else {
                        ps.setRoleId(rv.getRoleId());
                        try { ps.setRoleLevel(safeInt(rv.getLevel(), 1)); } catch (Throwable ignore) {}
                        return Mono.just(rv);
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("[login] flow error: {}", ex.toString());
                    return Mono.empty();
                })
                // 4) Gửi ACK sớm + TimeAck + RoleInfoAck → rồi bootstrap song song
                .flatMap(rv -> {
                    if (!StringUtils.hasText(ps.getRoleId())) {
                        // Không có role → coi như lỗi nhẹ
                        return sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                    }

                    // Gửi ACK sớm (Success)
                    Mono<Void> ackMono = sendLoginAck(ps, LOGIN_OK, 0);

                    // TimeAck ngay sau ACK
                    final int nowSec = (int)(System.currentTimeMillis()/1000L);
                    Emitters.sendTimeAck(ps, nowSec, /*openDays*/0);

                    // RoleInfoAck (nếu có dto)
                    if (rv != null) {
                        try { Emitters.sendRoleInfoAck(ps, rv); } catch (Throwable ignore) {}
                    }

                    final String rid = ps.getRoleId();
                    final String tk  = ps.getSessionId();

                    // ===== Starter box khi tạo role lần đầu =====
                    Mono<Void> starterGrant =
                            createdNow.get()
                                    ? loadStarterBoxCfg(tk)
                                    .flatMap(cfg -> grantStarterBox(tk, rid, cfg, ps))
                                    .onErrorResume(ex -> {
                                        log.warn("[login] grantStarterBox fail rid={}, ex={}", rid, ex.toString());
                                        return Mono.empty();
                                    })
                                    .then()
                                    : Mono.empty();

                    // ===== Bootstrap từng phần (song song) =====
                    Mono<Void> bagCommonMono = Mono.defer(() ->
                                    FeignCall.withToken(tk, "bag.get.common", () -> bagPublicFeign.get(rid, BAG_COMMON))
                            )
                            .doOnNext(bag -> {
                                try { Emitters.sendKnapsackAll(ps, bag); } catch (Throwable ignore) {}
                            })
                            .onErrorResume(ex -> {
                                log.warn("[bootstrap] bag.get COMMON ERROR rid={}, ex={}", rid, ex.toString());
                                return Mono.empty();
                            })
                            .then();

                    Mono<Void> bagEquipMono = Mono.defer(() ->
                                    FeignCall.withToken(tk, "bag.get.equip", () -> bagPublicFeign.get(rid, BAG_EQUIP))
                            )
                            .flatMap(bag -> sendEquipBagWithMeta(ps, tk, bag))
                            .onErrorResume(ex -> Mono.empty())
                            .then();

                    Mono<Void> equipMono = Mono.defer(() ->
                                    FeignCall.withToken(tk, "equip.list", () -> equipFeign.list(rid))
                            )
                            .doOnNext(list -> {
                                try {
                                    Emitters.sendEquipList(ps, list);
                                    try { ps.setRoleLevel(extractLevelFromEquipList(list, ps.getRoleLevel())); } catch (Throwable ignore) {}
                                } catch (Throwable ignore) {}
                            })
                            .onErrorResume(ex -> Mono.empty())
                            .then();

                    Mono<Void> walletMono = Mono.defer(() ->
                                    FeignCall.withToken(tk, "wallet.info", () -> walletFeign.info(rid))
                            )
                            .doOnNext(info -> {
                                try { Emitters.sendWalletAll(ps, info); } catch (Throwable ignore) {}
                            })
                            .onErrorResume(ex -> {
                                log.warn("[bootstrap] wallet.info ERROR rid={}, ex={}", rid, ex.toString());
                                return Mono.empty();
                            })
                            .then();

                    Mono<Void> boxInfoMono = Mono.defer(() ->
                                    FeignCall.withToken(tk, "box.info", () -> boxFeign.info(rid))
                            )
                            .doOnNext(info -> {
                                try { Emitters.sendBoxInfo(ps, info); } catch (Throwable ignore) {}
                            })
                            .onErrorResume(ex -> {
                                log.warn("[bootstrap] box.info ERROR rid={}, ex={}", rid, ex.toString());
                                return Mono.empty();
                            })
                            .then();

                    Mono<Void> boxSettingMono = Mono.defer(() ->
                                    FeignCall.withToken(tk, "box.getSetting", () -> boxFeign.getSetting(rid))
                            )
                            .doOnNext(set -> {
                                try { Emitters.sendBoxSettingInfo(ps, set); } catch (Throwable ignore) {}
                            })
                            .onErrorResume(ex -> {
                                log.warn("[bootstrap] box.getSetting ERROR rid={}, ex={}", rid, ex.toString());
                                return Mono.empty();
                            })
                            .then();

                    // NEW: đẩy SC_BOX_EQUIP_INFO rỗng để client sẵn sàng thao tác
                    Mono<Void> boxEquipInitMono = sendEmptyBoxEquipInfo(ps);

                    // (Tuỳ chọn) preload shop
                    Mono<Void> shopMono = Mono.defer(() ->
                                    FeignCall.withToken(tk, "shop.info", () -> shopFeign.info(rid, null, null))
                            )
                            .doOnNext(info -> {
                                try { Emitters.sendShopInfo(ps, info.data()); } catch (Throwable ignore) {}
                            })
                            .onErrorResume(ex -> Mono.empty())
                            .then();

                    return ackMono.then(
                            Mono.whenDelayError(
                                    starterGrant,
                                    bagCommonMono, bagEquipMono,
                                    equipMono, walletMono,
                                    boxInfoMono, boxSettingMono,
                                    shopMono
                            ).then()
                    );
                });
    }

    // ===================== Helpers =====================

    // ===== emit SC_BOX_EQUIP_INFO (rỗng) ngay sau login =====
    private Mono<Void> sendEmptyBoxEquipInfo(PlayerSession ps) {
        try {
            var equip = Msgequip.PB_EquipData.newBuilder()
                    .setItemId(0)
                    .setEquipType(-1)
                    .build();

            var sc = Msgbox.PB_SCBoxEquipInfo.newBuilder()
                    .setIsNew(0)
                    .setEquipInfo(equip)
                    .build();

            Emitters.emit(ps, MsgIds.SC_BOX_EQUIP_INFO, sc.toByteArray());
        } catch (Throwable ignore) {
            // không phá bootstrap nếu proto/field khác biệt
        }
        return Mono.empty();
    }

    private Mono<Void> sendLoginAck(PlayerSession ps, int result, int forbidSeconds) {
        var ack = Msglogin.PB_SCLoginToAccount.newBuilder()
                .setResult(result)
                .setForbidTime(forbidSeconds)
                .build();
        Emitters.emit(ps, MsgIds.SC_LOGIN_ACK, ack.toByteArray());
        return Mono.empty();
    }

    private Mono<Void> kickOldSessionIfAny(PlayerSession newPs) {
        try {
            if (!StringUtils.hasText(newPs.getUserId())) return Mono.empty();
            PlayerSession old = registry.get(newPs.getUserId()); // Yêu cầu SessionRegistry có API này
            if (old == null || old == newPs) return Mono.empty();

            // Gửi thông báo bị đá và đóng phiên cũ
            try { Emitters.sendDisconnectNotice(old, DISCONNECT_REASON_LOGIN_OTHER_PLACE); } catch (Throwable ignore) {}
            //try { old.close("Login from another place"); } catch (Throwable ignore) {}

            // Delay ngắn để chắc chắn client cũ rời
            return Mono.delay(Duration.ofMillis(800)).then();
        } catch (Throwable t) {
            log.warn("[login] kickOldSessionIfAny error: {}", t.toString());
            return Mono.empty();
        }
    }

    private RoleDTOs.CreateRoleReq buildDefaultCreateRoleReq(PlayerSession ps) {
        RoleDTOs.CreateRoleReq req = new RoleDTOs.CreateRoleReq();
        trySetString(req, "setUserId", ps.getUserId());
        trySetString(req, "setUid", ps.getUserId());
        trySetString(req, "setAccountId", ps.getUserId());
        String roleName = defaultRoleName(ps.getUsername(), ps.getUserId());
        trySetString(req, "setNickname", roleName);
        trySetString(req, "setRoleName", roleName);
        trySetString(req, "setName", roleName);
        trySetInt(req, "setJob", 1);
        trySetInt(req, "setClassId", 1);
        trySetInt(req, "setGender", 0);
        trySetString(req, "setServerId", "s1");
        return req;
    }

    private static String defaultRoleName(String username, String userId) {
        if (StringUtils.hasText(username)) return username;
        String suffix = (userId != null && userId.length() >= 4) ? userId.substring(userId.length() - 4) : "0000";
        return "Player_" + suffix;
    }

    private static void trySetString(Object target, String method, String value) {
        try { target.getClass().getMethod(method, String.class).invoke(target, value); } catch (Throwable ignore) {}
    }
    private static void trySetInt(Object target, String method, int value) {
        try { target.getClass().getMethod(method, int.class).invoke(target, value); } catch (Throwable ignore) {}
    }

    private static int safeInt(Object v, int def) {
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    private int extractLevelFromEquipList(Object equipListDto, int fallback) {
        // Tùy DTO của bạn, hiện trả fallback
        return fallback;
    }

    // ===== Forbid helper (reflection an toàn) =====
    private long calcForbidRemainSec(Object ir) {
        if (ir == null) return 0L;
        long nowSec = System.currentTimeMillis() / 1000L;
        Long until = invokeLongGetter(ir, "getForbidUntilEpochSec");
        if (until == null) until = invokeLongGetter(ir, "getForbidUntil");
        if (until == null) {
            // Một số hệ có getForbidTime() là epochSec
            until = invokeLongGetter(ir, "getForbidTime");
        }
        if (until == null || until <= 0) return 0L;
        long remain = until - nowSec;
        return Math.max(0L, remain);
    }

    private static Long invokeLongGetter(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            Object v = m.invoke(obj);
            if (v == null) return null;
            if (v instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(v));
        } catch (Throwable ignore) { return null; }
    }

    // ===== Starter box =====
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StarterBoxCfg {
        private long itemId;
        private int  count;
    }

    /**
     * Đọc roleexp.json -> other/Other/OtherCfg -> lấy box_id, box_num
     * Hỗ trợ object hoặc array[0]
     */
    private Mono<StarterBoxCfg> loadStarterBoxCfg(String token) {
        return FeignCall.withToken(token, "config.roleexp.raw", () -> configFeign.roleExpRaw())
                .map(resp -> resp != null ? resp.getBody() : null)
                .map(bytes -> {
                    if (bytes == null || bytes.length == 0) {
                        throw new IllegalStateException("roleexp.json empty");
                    }
                    try {
                        JsonNode root = JSON.readTree(bytes);
                        JsonNode other = root.get("other");
                        if (other == null) other = root.get("Other");
                        if (other == null) other = root.get("OtherCfg");
                        JsonNode node = (other != null && other.isArray() && other.size() > 0) ? other.get(0) : other;

                        long itemId = DEFAULT_BOX_ITEM_ID;
                        int count   = 0;

                        if (node != null) {
                            JsonNode boxId  = node.has("box_id")  ? node.get("box_id")  : node.get("boxId");
                            JsonNode boxNum = node.has("box_num") ? node.get("box_num") : node.get("boxNum");
                            if (boxId != null && !boxId.isNull()) {
                                itemId = boxId.isNumber() ? boxId.asLong() : Long.parseLong(boxId.asText());
                            }
                            if (boxNum != null && !boxNum.isNull()) {
                                count = boxNum.isNumber() ? boxNum.asInt() : Integer.parseInt(boxNum.asText());
                            }
                        }
                        return new StarterBoxCfg(itemId, Math.max(0, count));
                    } catch (Exception e) {
                        throw new IllegalStateException("Parse roleexp.json failed: " + e.getMessage(), e);
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("[login] loadStarterBoxCfg fail: {}", ex.toString());
                    return Mono.just(new StarterBoxCfg(DEFAULT_BOX_ITEM_ID, 0));
                });
    }

    /**
     * Validate meta -> add vào túi -> emit SC_KnapsackAllInfo + SC_KnapsackSingle
     */
    private Mono<Void> grantStarterBox(String token, String roleId, StarterBoxCfg cfg, PlayerSession ps) {
        if (cfg == null || cfg.getCount() <= 0 || cfg.getItemId() <= 0) return Mono.empty();

        return validateStarterItemViaConfig(token, cfg.getItemId())
                .flatMap(valid -> {
                    if (!valid) {
                        log.warn("[login] starter box invalid/virtual, skip rid={}, itemId={}", roleId, cfg.getItemId());
                        return Mono.empty();
                    }

                    var delta = new BagDTOs.ItemDelta(
                            (int) cfg.getItemId(),
                            cfg.getCount(),
                            false,
                            REASON_FIRST_LOGIN_BOX,
                            null,
                            true
                    );
                    var req = new BagDTOs.AddItemReq(roleId, BAG_COMMON, List.of(delta), SRC_MSG_LOGIN, SRC_OP_GRANT);

                    return FeignCall.withToken(token, "bag.add", () -> bagInternalFeign.add(req))
                            .doOnNext(resp -> log.info(
                                    "[login] Granted starter box rid={} itemId={} x{} added={} overflow={}",
                                    roleId, cfg.getItemId(), cfg.getCount(),
                                    (resp != null ? resp.getAdded() : null),
                                    (resp != null ? resp.getOverflow() : null)))
                            .onErrorResume(ex -> {
                                log.warn("[login] bag.add starter fail rid={}, ex={}", roleId, ex.toString());
                                return Mono.empty();
                            })
                            .then(Mono.defer(() ->
                                    FeignCall.withToken(token, "bag.get", () -> bagPublicFeign.get(roleId, BAG_COMMON))
                            ).doOnNext(bag -> {
                                try {
                                    Emitters.sendKnapsackAll(ps, bag);
                                    Emitters.sendKnapsackSingle(ps, bag, (int) cfg.getItemId());
                                } catch (Throwable ignore) {}
                            }).then());
                })
                .onErrorResume(ex -> {
                    log.warn("[login] validate starter item error rid={}, itemId={}, ex={}", roleId, cfg.getItemId(), ex.toString());
                    return Mono.empty();
                });
    }

    private Mono<Boolean> validateStarterItemViaConfig(String token, long itemId) {
        return FeignCall.withToken(token, "item.meta.batch",
                        () -> itemMetaFeign.batchMeta(Long.toString(itemId)))
                .map(resp -> {
                    Map<String, Object> m = (resp != null) ? resp.get(String.valueOf(itemId)) : null;
                    if (m == null) return false;                  // không có key -> invalid
                    if (m.containsKey("notFound")) return false;  // controller trả notFound=true
                    Object iv = m.get("isVirtual");
                    // Nếu thiếu isVirtual -> coi là virtual (def = 1)
                    return iv != null && toInt(iv, 1) == 0;
                })
                .onErrorReturn(false)
                .defaultIfEmpty(false);
    }

    private static int toInt(Object o, int def) {
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    // ===== (Optional) Tách Box bootstrap riêng nếu muốn dùng lại =====
    @SuppressWarnings("unused")
    private Mono<Void> pushBoxBootstrapNow(String token, PlayerSession ps) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();
        String rid = ps.getRoleId();

        Mono<Void> infoMono = FeignCall.withToken(token, "box.info", () -> boxFeign.info(rid))
                .doOnNext(info -> { try { Emitters.sendBoxInfo(ps, info); } catch (Throwable ignore) {} })
                .onErrorResume(ex -> {
                    log.warn("[bootstrap] box.info ERROR rid={}, ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then();

        Mono<Void> setMono = FeignCall.withToken(token, "box.getSetting", () -> boxFeign.getSetting(rid))
                .doOnNext(set -> { try { Emitters.sendBoxSettingInfo(ps, set); } catch (Throwable ignore) {} })
                .onErrorResume(ex -> {
                    log.warn("[bootstrap] box.getSetting ERROR rid={}, ex={}", rid, ex.toString());
                    return Mono.empty();
                })
                .then();

        return Mono.whenDelayError(infoMono, setMono);
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

    private String toCsvDistinctItemIds(BagDTOs.BagView bag) {
        return bag.getSlots().stream()
                .map(s -> String.valueOf(s.getItemId()))
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
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
}