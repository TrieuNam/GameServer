package com.southMillion.webSocket_server.service.client.session;

import org.SouthMillion.dto.session.DisconnectNoticeDTO;
import org.SouthMillion.dto.session.HeartbeatRequest;
import org.SouthMillion.dto.session.TimeAckDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "session-service")
public interface SessionFeignClient {

    @PostMapping("/api/session/heartbeat")
    void heartbeat(@RequestBody HeartbeatRequest req);

    @GetMapping("/api/session/time")
    TimeAckDTO getTime();

    @GetMapping("/api/session/{userId}/disconnect")
    DisconnectNoticeDTO getDisconnect(@PathVariable("userId") Long userId);
}
