package com.southMillion.session_service.controller;

import com.southMillion.session_service.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LoginDTOs;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService svc;
    private final Scheduler blockingScheduler;

    @PostMapping(value = "/api/session/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LoginDTOs.LoginResp>> login(
            @Valid @RequestBody LoginDTOs.LoginReq req,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff,
            @RequestHeader(value = "CF-Connecting-IP", required = false) String cfip,
            @RequestHeader(value = "X-Real-IP", required = false) String xrip) {

        String ip = firstNonBlank(cfip, xff, xrip, "unknown");
        return Mono.fromCallable(() -> ResponseEntity.ok(svc.login(req, ip)))
                .subscribeOn(blockingScheduler);
    }

    @PostMapping(value = "/api/session/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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