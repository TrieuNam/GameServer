package com.SouthMillion.webSocket_server.handler.session;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.DecodedPacket;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgentergs.Msgentergs;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatTimeHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HeartbeatTimeHandler.class);

    @Override
    public int[] interests() {
        return new int[]{ MsgIds.CS_HEARTBEAT_REQ, MsgIds.CS_TIME_REQ, MsgIds.CS_USER_LOGOUT };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        try {
            return switch (msgId) {
                case MsgIds.CS_HEARTBEAT_REQ -> {
                    // Có thể parse để validate:
                    // PB_CSHeartbeatReq.parseFrom(payload);
                    Emitters.sendHeartbeatResp(ps);
                    yield Mono.empty();
                }
                case MsgIds.CS_TIME_REQ -> {
                    // PB_CSTimeReq.parseFrom(payload);
                    int now = (int) (System.currentTimeMillis() / 1000L);
                    Emitters.sendTimeAck(ps, now, 0);
                    yield Mono.empty();
                }
                case MsgIds.CS_USER_LOGOUT -> {
                    // 1051 has no required fields; parse for protocol validation only.
                    Msgentergs.PB_CSUserLogout.parseFrom(payload);
                    ps.setLoggedIn(false);
                    Emitters.sendDisconnectNotice(ps, 0);
                    try {
                        ps.getWs().close().subscribe();
                    } catch (Throwable ignore) {
                        // Best-effort close.
                    }
                    yield Mono.empty();
                }
                default -> Mono.empty();
            };
        } catch (Throwable t) {
            log.warn("[heartbeat/time] error: {}", t.toString());
            return Mono.empty();
        }
    }
}