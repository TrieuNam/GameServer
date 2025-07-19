package com.southMillion.session_service.controller;

import org.SouthMillion.dto.session.HeartbeatRequest;
import com.southMillion.session_service.service.SessionService;
import org.SouthMillion.dto.session.OnlineStatusDto;
import org.SouthMillion.dto.session.SessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/session")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody HeartbeatRequest req) {
        sessionService.heartbeat(req.getSessionId(), req.getRoleId());
    }

    @DeleteMapping
    public void removeSession(@RequestParam String sessionId) {
        sessionService.removeSession(sessionId);
    }

    @GetMapping
    public SessionDto getSession(@RequestParam String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @GetMapping("/online-status")
    public OnlineStatusDto onlineStatus(@RequestParam Integer roleId) {
        boolean online = sessionService.isOnline(roleId);
        SessionDto session = sessionService.getSessionByRoleId(roleId);
        OnlineStatusDto dto = new OnlineStatusDto();
        dto.setRoleId(roleId);
        dto.setOnline(online);
        if (session != null) {
            dto.setSessionId(session.getSessionId());
            dto.setLastActive(session.getLastActive());
        }
        return dto;
    }
}