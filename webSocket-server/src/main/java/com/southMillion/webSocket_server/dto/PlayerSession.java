package com.SouthMillion.webSocket_server.dto;

import com.SouthMillion.webSocket_server.net.PacketCodec;
import lombok.*;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

@Getter @Builder
@AllArgsConstructor
public class PlayerSession {
    private final WebSocketSession ws;
    private final Sinks.Many<byte[]> outbound; // encoded frames to send
    @Setter private String sessionId; // Bearer token from handshake/payload
    @Setter private String analyticsSessionId; // Canonical sid used for analytics/logging
    @Setter private String userId;
    @Setter private Long roleId;
    @Setter private String roleName;
    @Setter private String username;

    /** True after successful login (CS:7056 validated). Guards all non-login messages. */
    @Setter private boolean loggedIn;
    @Setter private Integer currentSceneId;  // Current scene/map ID for world handler

    public void sendBinary(byte[] frame) {
        outbound.tryEmitNext(frame);
    }

    /**
     * Send a message with msgId and payload
     */
    public void send(int msgId, byte[] payload) {
        byte[] frame = PacketCodec.encode(msgId, payload);
        outbound.tryEmitNext(frame);
    }
}