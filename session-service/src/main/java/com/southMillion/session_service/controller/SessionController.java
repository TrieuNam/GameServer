package com.southMillion.session_service.controller;

import com.southMillion.session_service.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LoginDTOs;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService svc;
    private final Scheduler blockingScheduler;

    @PostMapping(value = "/api/session/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LoginDTOs.TokenPair>> login(@Valid @RequestBody LoginDTOs.LoginReq req) {
        return Mono.fromCallable(() -> svc.login(req.getUsername(), req.getPassword()))
                .map(tp -> tp == null
                        ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).<LoginDTOs.TokenPair>build()
                        : ResponseEntity.ok(tp))
                .subscribeOn(blockingScheduler);
    }

    @PostMapping("/api/session/refresh")
    public Mono<ResponseEntity<LoginDTOs.TokenPair>> refresh(@RequestBody LoginDTOs.RefreshReq req) {
        return Mono.fromCallable(() -> svc.refresh(req.getRefreshToken()))
                .map(tp -> tp == null
                        ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).<LoginDTOs.TokenPair>build()
                        : ResponseEntity.ok(tp))
                .subscribeOn(blockingScheduler);
    }

    @PostMapping("/api/session/introspect")
    public Mono<ResponseEntity<LoginDTOs.IntrospectResp>> introspect(
            @RequestHeader("Authorization") String auth) {
        return Mono.fromCallable(() -> {
                    if (auth == null || !auth.startsWith("Bearer ")) {
                        return ResponseEntity.badRequest().<LoginDTOs.IntrospectResp>build();
                    }
                    String token = auth.substring("Bearer ".length());
                    return ResponseEntity.ok(svc.introspect(token));
                })
                .subscribeOn(blockingScheduler);
    }
}