package com.southMillion.webSocket_server.handler.login;

import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.service.SessionRegistry;
import com.southMillion.webSocket_server.service.client.*;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoginBootstrapHandler implements MessageHandler {

    // ===== Feign / Registry =====
    private final SessionFeignClient sessionFeign;
    private final RoleHttpClient     roleFeign;
    private final SessionRegistry    registry;

    // ===== Guard =====
    private static final Semaphore LOGIN_LIMITER = new Semaphore(128);
    private static volatile boolean WORLD_READY  = true;

    // ===== Result codes (match client)
    private static final int LOGIN_OK                    = 0;
    private static final int LOGIN_ERR_MISSING_TOKEN     = 2;
    private static final int LOGIN_ERR_PARSE             = 4;
    private static final int LOGIN_ERR_SERVER_NOTREADY   = 11;
    private static final int LOGIN_ERR_SERVER_BUSY       = 12;
    private static final int LOGIN_ERR_FORBID            = 13;
    @SuppressWarnings("unused")
    private static final int LOGIN_ERR_FORBID_NEWROLE    = 14;

    private static final int DISCONNECT_REASON_LOGIN_OTHER_PLACE = 1;

    @Override
    public int[] interests() {
        // gộp 3 msg nhóm 1: login + time + heartbeat
        return new int[] { MsgIds.CS_LOGIN_REQ, MsgIds.CS_HEARTBEAT_REQ, MsgIds.CS_TIME_REQ };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        try {
            return switch (msgId) {
                case MsgIds.CS_LOGIN_REQ -> handleLogin(ps, payload);
                case MsgIds.CS_HEARTBEAT_REQ -> { Emitters.sendHeartbeatResp(ps); yield Mono.empty(); }
                case MsgIds.CS_TIME_REQ -> {
                    int now = (int)(System.currentTimeMillis() / 1000L);
                    Emitters.sendTimeAck(ps, now, 0);
                    yield Mono.empty();
                }
                default -> Mono.empty();
            };
        } catch (Throwable t) {
            log.warn("[handler] error msgId={}, ex={}", msgId, t.toString());
            return Mono.empty();
        }
    }

    private Mono<Void> handleLogin(PlayerSession ps, byte[] payload) {
        if (!WORLD_READY) {
            Emitters.sendLoginAck(ps, LOGIN_ERR_SERVER_NOTREADY, 0);
            return Mono.empty();
        }
        if (!LOGIN_LIMITER.tryAcquire()) {
            Emitters.sendLoginAck(ps, LOGIN_ERR_SERVER_BUSY, 0);
            return Mono.empty();
        }

        final Msglogin.PB_CSLoginToAccount req;
        try {
            req = Msglogin.PB_CSLoginToAccount.parseFrom(payload);
        } catch (Exception e) {
            log.warn("[login] parse 7056 error: {}", e.toString());
            Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
            LOGIN_LIMITER.release();
            return Mono.empty();
        }

        final String token = (req != null && req.hasLoginStr()) ? req.getLoginStr() : null;
        if (!org.springframework.util.StringUtils.hasText(token)) {
            log.warn("[login] missing login_str");
            Emitters.sendLoginAck(ps, LOGIN_ERR_MISSING_TOKEN, 0);
            LOGIN_LIMITER.release();
            return Mono.empty();
        }

        final java.util.concurrent.atomic.AtomicBoolean createdNow = new java.util.concurrent.atomic.AtomicBoolean(false);

        return FeignCall.withToken(token, "session.introspect", () -> sessionFeign.introspect(token))
                .doOnSubscribe(s -> log.info("[login] introspect start"))
                .doOnError(e -> log.warn("[login] introspect error: {}", e.toString()))
                .switchIfEmpty(Mono.error(new IllegalStateException("introspect returned EMPTY")))
                .flatMap(ir -> {
                    long forbidRemain = calcForbidRemainSec(ir);
                    if (forbidRemain > 0) {
                        Emitters.sendLoginAck(ps, LOGIN_ERR_FORBID, (int) forbidRemain);
                        return Mono.empty();
                    }

                    // bind session
                    ps.setLoggedIn(true);
                    ps.setSessionId(token);
                    ps.setUserId(safeStr(invokeStr(ir, "getUserId"), ""));
                    ps.setUsername(safeStr(invokeStr(ir, "getUsername"), ""));
                    registry.updateBindings(ps);

                    if (!org.springframework.util.StringUtils.hasText(ps.getUserId())) {
                        log.warn("[login] introspect ok nhưng userId rỗng");
                        Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                        return Mono.empty();
                    }

                    // đảm bảo luôn complete, tránh treo
                    return kickOldSessionIfAny(ps)
                            .timeout(java.time.Duration.ofSeconds(3), Mono.empty())
                            .onErrorResume(e -> {
                                log.warn("[login] kickOldSessionIfAny error: {}", e.toString());
                                return Mono.empty();
                            })
                            .then(
                                    // role.list: map 404/null/empty -> ListResp(items=[])
                                    FeignCall.withToken(ps.getSessionId(), "role.list", () -> roleFeign.list(ps.getUserId()))
                                            .doOnSubscribe(s -> log.info("[login] role.list start for user {}", ps.getUserId()))
                                            .onErrorResume(feign.FeignException.NotFound.class, e -> {
                                                log.info("[login] role.list 404 -> treat as empty list");
                                                return Mono.just(RoleDTOs.ListResp.builder().items(java.util.List.of()).build());
                                            })
                                            .switchIfEmpty(Mono.fromSupplier(() -> {
                                                log.info("[login] role.list returned EMPTY -> treat as []");
                                                return RoleDTOs.ListResp.builder().items(java.util.List.of()).build();
                                            }))
                                            .doOnNext(list -> {
                                                int n = (list == null || list.getItems() == null) ? 0 : list.getItems().size();
                                                log.info("[login] role.list ok: {} items", n);
                                            })
                            );
                })
                .flatMap(list -> {
                    RoleDTOs.RoleResp rv = (list != null && list.getItems() != null && !list.getItems().isEmpty())
                            ? list.getItems().get(0) : null;

                    if (rv == null) {
                        var createReq = buildDefaultCreateRoleReq(ps); // trả RoleDTOs.CreateRoleReq
                        return FeignCall.withToken(ps.getSessionId(), "role.create", () -> roleFeign.create(createReq))
                                .doOnSubscribe(s -> log.info("[login] role.create start"))
                                .switchIfEmpty(Mono.error(new IllegalStateException("role.create returned EMPTY")))
                                .onErrorResume(feign.FeignException.NotFound.class, e ->
                                        Mono.error(new IllegalStateException("role.create 404 (sai mapping hoặc content-type?)", e)))
                                .doOnNext(newRole -> log.info("[login] role.create ok: {}", safeStr(newRole.getRoleId(), "")))
                                .map(newRole -> { bindRoleToSession(ps, newRole); createdNow.set(true); return newRole; });
                    } else {
                        bindRoleToSession(ps, rv);
                        return Mono.just(rv);
                    }
                })
                .flatMap(rv -> {
                    if (!org.springframework.util.StringUtils.hasText(ps.getRoleId())) {
                        log.warn("[login] roleId rỗng sau khi bind");
                        Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                        return Mono.empty();
                    }
                    Emitters.sendLoginAck(ps, LOGIN_OK, 0);
                    int now = (int) (System.currentTimeMillis() / 1000L);
                    Emitters.sendTimeAck(ps, now, 0);
                    Emitters.sendRoleInfoAck(ps, rv);

                    if (createdNow.get()) {
                        log.info("[login] new role created for user {}", ps.getUserId());
                        // có thể emit thêm gói tân thủ tại đây nếu muốn
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.warn("[login] flow error -> ACK PARSE: {}", e.toString());
                    Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                    return Mono.empty();
                })
                .doFinally(sig -> LOGIN_LIMITER.release())
                .then();
    }

    /* ================= Helpers ================= */

    private Mono<Void> kickOldSessionIfAny(PlayerSession newPs) {
        try {
            if (!StringUtils.hasText(newPs.getUserId())) return Mono.empty();
            PlayerSession old = registry.get(newPs.getUserId());
            if (old == null || old == newPs) return Mono.empty();

            try { Emitters.sendDisconnectNotice(old, DISCONNECT_REASON_LOGIN_OTHER_PLACE); } catch (Throwable ignore) {}
            // đóng phiên cũ sau một nhịp nhỏ; nếu có API close, gọi tại đây
            return Mono.delay(Duration.ofMillis(600)).then();
        } catch (Throwable t) {
            log.warn("[login] kickOldSessionIfAny error: {}", t.toString());
            return Mono.empty();
        }
    }

    private void bindRoleToSession(PlayerSession ps, RoleDTOs.RoleResp rv) {
        if (rv == null) return;
        ps.setRoleId(safeStr(rv.getRoleId(), null));
        int lv = 1;
        try { lv = Integer.parseInt(String.valueOf(rv.getLevel())); } catch (Exception ignore) {}
        ps.setRoleLevel(lv);
    }

    private RoleDTOs.CreateRoleReq buildDefaultCreateRoleReq(PlayerSession ps) {
        RoleDTOs.CreateRoleReq req = new RoleDTOs.CreateRoleReq();
        trySet(req, "setUserId", ps.getUserId());
        trySet(req, "setUid", ps.getUserId());
        trySet(req, "setAccountId", ps.getUserId());
        String roleName = defaultRoleName(ps.getUsername(), ps.getUserId());
        trySet(req, "setNickname", roleName);
        trySet(req, "setRoleName", roleName);
        trySet(req, "setName", roleName);
        trySet(req, "setJob", 1);
        trySet(req, "setClassId", 1);
        trySet(req, "setGender", 0);
        trySet(req, "setServerId", "s1");
        return req;
    }

    private String defaultRoleName(String username, String userId) {
        if (StringUtils.hasText(username)) return username;
        String suffix = (userId != null && userId.length() >= 4) ? userId.substring(userId.length() - 4) : "0000";
        return "Player_" + suffix;
    }

    private long calcForbidRemainSec(Object ir) {
        if (ir == null) return 0L;
        long nowSec = System.currentTimeMillis() / 1000L;
        Long until = invokeLong(ir, "getForbidUntilEpochSec");
        if (until == null) until = invokeLong(ir, "getForbidUntil");
        if (until == null) until = invokeLong(ir, "getForbidTime");
        if (until == null || until <= 0) return 0L;
        long remain = until - nowSec;
        return Math.max(0L, remain);
    }

    private void trySet(Object bean, String setter, Object val) {
        try {
            var m = bean.getClass().getMethod(setter, val.getClass());
            m.invoke(bean, val);
        } catch (NoSuchMethodException e) {
            try {
                if (val instanceof Integer i) {
                    var m = bean.getClass().getMethod(setter, int.class); m.invoke(bean, i.intValue());
                } else if (val instanceof Long l) {
                    var m = bean.getClass().getMethod(setter, long.class); m.invoke(bean, l.longValue());
                } else if (val instanceof Boolean b) {
                    var m = bean.getClass().getMethod(setter, boolean.class); m.invoke(bean, b.booleanValue());
                } else if (val instanceof String s) {
                    var m = bean.getClass().getMethod(setter, String.class); m.invoke(bean, s);
                }
            } catch (Exception ignore) {}
        } catch (Exception ignore) {}
    }

    private String safeStr(String s, String def) { return (s == null || s.isBlank()) ? def : s; }

    private Long invokeLong(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object v = m.invoke(obj);
            if (v == null) return null;
            if (v instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) { return null; }
    }

    private String invokeStr(Object obj, String method) {
        try {
            var m = obj.getClass().getMethod(method);
            Object v = m.invoke(obj);
            return v == null ? null : String.valueOf(v);
        } catch (Exception e) { return null; }
    }
}