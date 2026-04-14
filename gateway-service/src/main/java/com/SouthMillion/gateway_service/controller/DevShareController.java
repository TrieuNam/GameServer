package com.SouthMillion.gateway_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DevShareController {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;

    @GetMapping("/api/dev/share_user_play")
    public Mono<ResponseEntity<Map<String, Object>>> shareUserPlay(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "share_userId", required = false) Long shareUserId,
            @RequestParam(name = "roleId", required = false) Long roleId,
            @RequestParam(name = "share_roleId", required = false) Long shareRoleId,
            @RequestParam(name = "share_serverId", required = false) Integer shareServerId) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("shareUserId", shareUserId);
        payload.put("roleId", roleId);
        payload.put("shareRoleId", shareRoleId);
        payload.put("shareServerId", shareServerId);

        return webClient.post()
                .uri("lb://activity-service/api/activity/internal/friend-invite/share-play")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(resp -> {
                    Map<String, Object> out = new HashMap<>();
                    out.put("ret", 0);
                    out.put("data", resp);
                    return ResponseEntity.ok(out);
                })
                .onErrorResume(ex -> {
                    log.error("[DevShare] share_user_play forward failed userId={} shareUserId={} roleId={} shareRoleId={} shareServerId={}",
                            userId, shareUserId, roleId, shareRoleId, shareServerId, ex);
                    Map<String, Object> out = new HashMap<>();
                    out.put("ret", -1);
                    out.put("msg", "share_user_play_forward_failed");
                    return Mono.just(ResponseEntity.ok(out));
                });
    }
}
