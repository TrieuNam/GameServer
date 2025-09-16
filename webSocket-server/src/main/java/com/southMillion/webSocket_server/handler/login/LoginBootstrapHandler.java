package com.southMillion.webSocket_server.handler.login;

import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.service.InMemoryPlayerSessionRegistry;
import com.southMillion.webSocket_server.service.client.*;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.proto.Msglogin.Msglogin;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoginBootstrapHandler implements MessageHandler {

    // ===== Feign / Registry =====
    private final SessionFeignClient sessionFeign; // bạn đã có interface này trong project
    private final RoleFeign roleFeign;
    private final InMemoryPlayerSessionRegistry registry;

    // ===== Guard =====
    private static final Semaphore LOGIN_LIMITER = new Semaphore(128);
    private static volatile boolean WORLD_READY = true;

    // ===== Result codes (match client)
    private static final int LOGIN_OK = 0;
    private static final int LOGIN_ERR_MISSING_TOKEN = 2;
    private static final int LOGIN_ERR_PARSE = 4;
    private static final int LOGIN_ERR_SERVER_NOTREADY = 11;
    private static final int LOGIN_ERR_SERVER_BUSY = 12;
    private static final int LOGIN_ERR_FORBID = 13;

    private static final int DISCONNECT_REASON_LOGIN_OTHER_PLACE = 1;

    @Override
    public int[] interests() {
        return new int[]{ MsgIds.CS_LOGIN_REQ };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (msgId != MsgIds.CS_LOGIN_REQ) return Mono.empty();
        return handleLogin(ps, payload);
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

        final String token = req.hasLoginStr() ? req.getLoginStr() : ps.getSessionId();
        if (token == null || token.isBlank()) {
            log.warn("[login] missing login_str");
            Emitters.sendLoginAck(ps, LOGIN_ERR_MISSING_TOKEN, 0);
            LOGIN_LIMITER.release();
            return Mono.empty();
        }

        final var createdNow = new java.util.concurrent.atomic.AtomicBoolean(false);

        return FeignCall.withToken(token, "session.introspect", () -> sessionFeign.introspect(token))
                .switchIfEmpty(Mono.error(new IllegalStateException("introspect EMPTY")))
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

                    if (ps.getUserId() == null || ps.getUserId().isBlank()) {
                        log.warn("[login] introspect ok nhưng userId rỗng");
                        Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                        return Mono.empty();
                    }

                    // đá phiên cũ nếu có
                    return kickOldSessionIfAny(ps)
                            .timeout(Duration.ofSeconds(3), Mono.empty())
                            .onErrorResume(e -> Mono.empty())
                            .then(FeignCall.withToken(ps.getSessionId(), "role.listByUser",
                                            () -> roleFeign.listByUser(ps.getUserId()))
                                    .switchIfEmpty(Mono.just(List.of())));
                })
                .flatMap(list -> {
                    RoleDTOs.RoleResp role = (list != null && !list.isEmpty()) ? pickRole(list) : null;

                    if (role == null) {
                        var createReq = buildCreateRoleReq(ps, req);
                        return FeignCall.withToken(ps.getSessionId(), "role.create",
                                        () -> roleFeign.create(createReq))
                                .doOnNext(newRole -> {
                                    registry.bindRoleToSession(ps, newRole.getRoleId(), newRole.getUserId(), newRole.getName());
                                    createdNow.set(true);
                                });
                    } else {
                        registry.bindRoleToSession(ps, role.getRoleId(), role.getUserId(), role.getName());
                        return Mono.just(role);
                    }
                })
                .flatMap(role -> {
                    if (ps.getRoleId() == null || ps.getRoleId().isBlank()) {
                        log.warn("[login] roleId rỗng sau khi bind");
                        Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                        return Mono.empty();
                    }
                    // ACKs
                    Emitters.sendLoginAck(ps, LOGIN_OK, 0);
                    int now = (int) (System.currentTimeMillis() / 1000L);
                    Emitters.sendTimeAck(ps, now, 0);
                    Emitters.sendRoleInfoAck(ps, role);
                    if (createdNow.get()) {
                        log.info("[login] created new role for user {}", ps.getUserId());
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.warn("[login] flow error -> ACK PARSE: {}", e.toString());
                    Emitters.sendLoginAck(ps, LOGIN_ERR_PARSE, 0);
                    return Mono.empty();
                })
                .doFinally(sig -> LOGIN_LIMITER.release()).then();
    }

    /* ================= Helpers ================= */

    private RoleDTOs.RoleResp pickRole(List<RoleDTOs.RoleResp> list) {
        return list.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(RoleDTOs.RoleResp::getLevel, Comparator.nullsFirst(Integer::compareTo)))
                .orElse(list.get(0));
    }

    private RoleDTOs.CreateRoleReq buildCreateRoleReq(PlayerSession ps, Msglogin.PB_CSLoginToAccount req) {
        var r = new RoleDTOs.CreateRoleReq();
        tryInvokeSetter(r, "setUserId", ps.getUserId());
        String name = (req.hasPname() && !req.getPname().isBlank())
                ? req.getPname()
                : defaultRoleName(ps.getUserId());
        tryInvokeSetter(r, "setName", name);
        tryInvokeSetter(r, "setNickname", name);
        tryInvokeSetter(r, "setRoleName", name);
        int serverId = req.hasServer() ? req.getServer() : 1;
        tryInvokeSetter(r, "setServerId", serverId);
        return r;
    }

    private String defaultRoleName(String userId) {
        String suffix = (userId != null && userId.length() >= 4)
                ? userId.substring(userId.length() - 4) : "0000";
        return "Player_" + suffix;
    }

    private Mono<Void> kickOldSessionIfAny(PlayerSession newPs) {
        var others = registry.sessionsOfUser(newPs.getUserId()).stream()
                .filter(s -> !s.getWs().getId().equals(newPs.getWs().getId()))
                .toList();
        if (others.isEmpty()) return Mono.empty();
        for (var o : others) {
            try { Emitters.sendDisconnectNotice(o, DISCONNECT_REASON_LOGIN_OTHER_PLACE); } catch (Throwable ignore) {}
            try { o.getWs().close().subscribe(); } catch (Throwable ignore) {}
        }
        return Mono.empty();
    }

    private long calcForbidRemainSec(Object ir) {
        long now = System.currentTimeMillis() / 1000L;
        Long until = invokeLong(ir, "getForbidUntilEpochSec");
        if (until == null) until = invokeLong(ir, "getForbidUntil");
        if (until == null) until = invokeLong(ir, "getForbidTime");
        if (until == null || until <= 0) return 0;
        return Math.max(0, until - now);
    }

    private String extractUserId(Object ir) {
        String s = invokeStr(ir, "getUserId");
        if (s == null) s = invokeStr(ir, "userId");
        return s;
    }

    private void tryInvokeSetter(Object bean, String setter, Object val) {
        try {
            Method m = null;
            try { m = bean.getClass().getMethod(setter, val.getClass()); }
            catch (NoSuchMethodException ignore) {}
            if (m == null && val instanceof Integer i) m = bean.getClass().getMethod(setter, int.class);
            if (m == null && val instanceof Long l)   m = bean.getClass().getMethod(setter, long.class);
            if (m == null && val instanceof Boolean b)m = bean.getClass().getMethod(setter, boolean.class);
            if (m == null && val instanceof String s) m = bean.getClass().getMethod(setter, String.class);
            if (m != null) m.invoke(bean, val);
        } catch (Exception ignore) {}
    }

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

    private String safeStr(String s, String def) { return (s == null || s.isBlank()) ? def : s; }

}