package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.session.HeartbeatRequest;
import org.SouthMillion.dto.session.OnlineStatusDto;
import org.SouthMillion.dto.session.SessionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "session-service")
public interface SessionFeignClient {
    @PostMapping("/session/heartbeat")
    void heartbeat(@RequestBody HeartbeatRequest req);

    @DeleteMapping("/session")
    void removeSession(@RequestParam("sessionId") String sessionId);

    @GetMapping("/session")
    SessionDto getSession(@RequestParam("sessionId") String sessionId);

    @GetMapping("/session/online-status")
    OnlineStatusDto onlineStatus(@RequestParam("roleId") Integer roleId);
}