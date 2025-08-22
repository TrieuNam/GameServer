package com.southMillion.webSocket_server.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

@Data
@Builder
public class PlayerSession {
    private String userId;
    private String username;
    private String sessionId;
    private String roleId;

    private WebSocketSession ws;

    // ✨ Outbound queue (đơn phát cho 1 ws)
    private Sinks.Many<byte[]> outbound;

    private boolean loggedIn;
}