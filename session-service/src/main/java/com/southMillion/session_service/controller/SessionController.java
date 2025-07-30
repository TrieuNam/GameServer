package com.southMillion.session_service.controller;

import org.SouthMillion.dto.session.*;
import com.southMillion.session_service.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    @Autowired
    private SessionService service;

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody HeartbeatRequest req) {
        service.updateHeartbeat(req.getSessionId(), req.getRoleId());
    }

    @GetMapping("/time")
    public TimeAckDTO getTime() {
        return service.getTimeAck();
    }

    @GetMapping("/{userId}/disconnect")
    public DisconnectNoticeDTO getDisconnect(@PathVariable Long userId) {
        return service.getDisconnectInfo(userId);
    }
}