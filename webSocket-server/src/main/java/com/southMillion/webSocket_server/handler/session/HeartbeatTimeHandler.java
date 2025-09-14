package com.southMillion.webSocket_server.handler.session;

import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.DecodedPacket;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatTimeHandler implements MessageHandler {

    @Override
    public int[] interests() {
        return new int[]{ MsgIds.CS_HEARTBEAT_REQ, MsgIds.CS_TIME_REQ };
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
                default -> Mono.empty();
            };
        } catch (Throwable t) {
            log.warn("[heartbeat/time] error: {}", t.toString());
            return Mono.empty();
        }
    }
}