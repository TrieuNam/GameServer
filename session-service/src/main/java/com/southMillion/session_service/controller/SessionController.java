package com.SouthMillion.session_service.controller;

import com.SouthMillion.session_service.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LoginDTOs;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService svc;
    private final Scheduler blockingScheduler;

    /**
     * Frontend expects: { ret: 0, user: { account, uid, login_sign, ... }, role_data: {...} }
     * We wrap LoginResp into that shape so existing client code works unchanged.
     *
     * consumes = "*\/application/json*" — accepts both "application/json" AND
     * "application/json;charset=utf-8" sent by the TypeScript HttpHelper.
     */
    @PostMapping(value = "/api/session/login",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> login(
            @Valid @RequestBody LoginDTOs.LoginReq req,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff,
            @RequestHeader(value = "CF-Connecting-IP", required = false) String cfip,
            @RequestHeader(value = "X-Real-IP", required = false) String xrip) {

        String ip = firstNonBlank(cfip, xff, xrip, "unknown");
        return Mono.fromCallable(() -> {
            LoginDTOs.LoginResp resp = svc.login(req, ip);

            // Build user object matching frontend LoginVerify.User structure
            Map<String, Object> user = new HashMap<>();
            user.put("account",       resp.getAccount());
            user.put("uid",           resp.getUserId());
            user.put("login_sign",    resp.getAccessToken());   // frontend uses login_sign to connect game-server
            user.put("login_time",    System.currentTimeMillis() / 1000L);
            user.put("account_type",  1);
            user.put("fcm_flag",      0);
            user.put("openid",        resp.getUserId());
            user.put("merger_spid",   "");
            user.put("spid",          "");
            user.put("account_spid",  "");
            // Also expose token fields at top level for convenience
            user.put("accessToken",   resp.getAccessToken());
            user.put("refreshToken",  resp.getRefreshToken());
            user.put("sessionId",     resp.getSessionId());
            user.put("expiresAt",     resp.getExpiresAt());
            user.put("tokenType",     resp.getTokenType());

            Map<String, Object> result = new HashMap<>();
            result.put("ret",       0);
            result.put("msg",       "ok");
            result.put("user",      user);
            result.put("role_data", new HashMap<>());   // populated by game-server after TCP login

            return ResponseEntity.ok(result);
        }).subscribeOn(blockingScheduler);
    }

    @PostMapping(value = "/api/session/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LoginDTOs.RefreshResp>> refresh(
            @Valid @RequestBody LoginDTOs.RefreshReq req,
            @RequestHeader(value = "X-User-Id", required = false) String uidForRl) {
        // uidForRl không bắt buộc; chỉ dùng để rate-limit tốt hơn nếu bạn truyền qua gateway
        return Mono.fromCallable(() -> ResponseEntity.ok(svc.refresh(req, uidForRl)))
                .subscribeOn(blockingScheduler);
    }

    @PostMapping("/api/session/heartbeat")
    public Mono<ResponseEntity<Void>> heartbeat(@RequestHeader("Authorization") String auth) {
        return Mono.fromCallable(() -> {
            svc.heartbeat(extractBearer(auth));
            return ResponseEntity.noContent().<Void>build();
        }).subscribeOn(blockingScheduler);
    }

    @PostMapping("/api/session/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestHeader("Authorization") String auth) {
        return Mono.fromCallable(() -> {
            svc.logout(extractBearer(auth));
            return ResponseEntity.noContent().<Void>build();
        }).subscribeOn(blockingScheduler);
    }

    @GetMapping(value = "/api/session/time", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LoginDTOs.TimeResp>> time() {
        return Mono.fromCallable(() ->
                        ResponseEntity.ok(LoginDTOs.TimeResp.builder()
                                .serverTime(Instant.now().toEpochMilli())
                                .build()))
                .subscribeOn(blockingScheduler);
    }

    @PostMapping("/internal/session/introspect")
    public Mono<ResponseEntity<LoginDTOs.IntrospectResp>> introspect(
            @RequestHeader("Authorization") String auth) {
        return Mono.fromCallable(() -> {
                    String token = extractBearer(auth);
                    return ResponseEntity.ok(svc.introspect(token));
                })
                .subscribeOn(blockingScheduler);
    }

    private static String extractBearer(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("missing_bearer");
        }
        return auth.substring("Bearer ".length());
    }

    private static String firstNonBlank(String... xs) {
        for (String x : xs) if (x != null && !x.isBlank()) return x.split(",")[0].trim();
        return "unknown";
    }
}