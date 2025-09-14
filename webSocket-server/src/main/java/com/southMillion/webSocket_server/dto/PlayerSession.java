package com.southMillion.webSocket_server.dto;

import lombok.*;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

@Getter @Builder
@AllArgsConstructor
public class PlayerSession {
    private final WebSocketSession ws;
    private final Sinks.Many<byte[]> outbound; // encoded frames to send
    @Setter private String sessionId; // Bearer token from handshake/payload
    @Setter private String userId;
    @Setter private String roleId;
    @Setter private String roleName;
    @Setter private String username;

    @Setter private boolean loggedIn;

    public void sendBinary(byte[] frame) {
        outbound.tryEmitNext(frame);
    }
}