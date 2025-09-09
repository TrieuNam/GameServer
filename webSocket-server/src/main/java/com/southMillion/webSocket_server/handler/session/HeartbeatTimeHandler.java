package com.southMillion.webSocket_server.handler.session;

import com.southMillion.webSocket_server.dto.PlayerSession;
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

    @Override public int[] interests() {
        return new int[]{ MsgIds.CS_HEARTBEAT_REQ, MsgIds.CS_TIME_REQ };
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (msgId == MsgIds.CS_HEARTBEAT_REQ) {
            Emitters.sendHeartbeatResp(ps);
            return Mono.empty();
        }
        if (msgId == MsgIds.CS_TIME_REQ) {
            int now = (int)(System.currentTimeMillis()/1000);
            Emitters.sendTimeAck(ps, now, /*openDays*/0);
            return Mono.empty();
        }
        return Mono.empty();
    }
}