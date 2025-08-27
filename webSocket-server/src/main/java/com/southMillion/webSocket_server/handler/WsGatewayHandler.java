package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.config.HandlerRegistry;
import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.PacketCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.ByteBuffer;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsGatewayHandler implements WebSocketHandler {

    private final HandlerRegistry registry;

    @Override public Mono<Void> handle(WebSocketSession session) {
        var ps = PlayerSession.builder()
                .ws(session)
                .sessionId(null)      // sẽ set sau login
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();

        Mono<Void> send = session.send(
                ps.getOutbound().asFlux()
                        .map(bytes -> session.binaryMessage(buf -> buf.wrap(bytes)))
        ).doFinally(sig -> ps.getOutbound().tryEmitComplete());

        Mono<Void> recv = session.receive()
                .map(WebSocketMessage::getPayload)
                .map(buf -> {
                    ByteBuffer bb = buf.asByteBuffer();
                    byte[] dst = new byte[bb.remaining()];
                    bb.get(dst);
                    return PacketCodec.decode(dst);
                })
                .flatMap(decoded -> dispatch(ps, decoded.msgId(), decoded.payload()))
                .onErrorResume(ex -> {
                    log.warn("WS recv error: {}", ex.toString());
                    return Mono.empty();
                })
                .then();

        return Mono.when(send, recv);
    }

    private Mono<Void> dispatch(PlayerSession ps, int msgId, byte[] payload) {
        Optional<MessageHandler> h = registry.find(msgId);
        if (h.isEmpty()) {
            log.warn("Unknown MsgId {}", msgId);
            return Mono.empty();
        }
        return h.get().handle(ps, msgId, payload)
                .onErrorResume(ex -> {
                    log.warn("Handler {} error: {}", h.get().getClass().getSimpleName(), ex.toString());
                    return Mono.empty();
                });
    }
}