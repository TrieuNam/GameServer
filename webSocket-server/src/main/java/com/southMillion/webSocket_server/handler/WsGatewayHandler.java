package com.SouthMillion.webSocket_server.handler;

import com.SouthMillion.webSocket_server.config.HandlerRegistry;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.analytics.AnalyticsHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.InMemoryPlayerSessionRegistry;
import com.SouthMillion.webSocket_server.utils.FeignTokenHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsGatewayHandler implements WebSocketHandler {

    private final HandlerRegistry registry;
    private final InMemoryPlayerSessionRegistry sessionRegistry;
    private final AnalyticsHandler analyticsHandler;

    /**
     * Virtual-thread scheduler để offload blocking Feign/gRPC calls khỏi Netty event-loop.
     * ⚠️ CRITICAL: thiếu dòng này → 1 service chết = toàn bộ WebSocket server bị đứng.
     */
    @Autowired
    @Qualifier("feignVtScheduler")
    private Scheduler feignScheduler;

    /** Timeout mặc định cho mỗi handler — tổng gRPC deadline + Feign timeout không vượt quá con số này */
    private static final Duration HANDLER_TIMEOUT = Duration.ofSeconds(10);

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var ps = PlayerSession.builder()
                .ws(session)
                .sessionId(null)
                // multicast: thread-safe cho concurrent tryEmitNext() từ nhiều virtual threads
                // unicast chỉ cho phép 1 thread emit tại một thời điểm → messages bị drop
                // khi Mono.when() chạy nhiều pushAll() song song
                .outbound(Sinks.many().multicast().onBackpressureBuffer(256, false))
                .build();

        // Đăng ký ngay khi mở WS
        sessionRegistry.put(ps);

        // Track WS_CONNECT
        analyticsHandler.track(ps, "WS_CONNECT", "SYSTEM",
                Map.of("wsId", session.getId()));

        Mono<Void> send = session.send(
                ps.getOutbound().asFlux().map(bytes -> session.binaryMessage(buf -> buf.wrap(bytes)))
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
                    // Track lỗi nhận message
                    analyticsHandler.track(ps, "WS_RECV_ERROR", "ERROR",
                            Map.of("error", ex.getClass().getSimpleName(), "msg", ex.getMessage() != null ? ex.getMessage() : ""));
                    return Mono.empty();
                })
                .then();

        return Mono.when(send, recv)
                .doFinally(sig -> {
                    // Track WS_DISCONNECT
                    analyticsHandler.track(ps, "WS_DISCONNECT", "SYSTEM",
                            Map.of("wsId", session.getId(), "signal", sig.toString()));
                    sessionRegistry.remove(ps);
                });
    }

    private Mono<Void> dispatch(PlayerSession ps, int msgId, byte[] payload) {
        // ── Login guard: chỉ CS_LOGIN_REQ (7056) và CS_HEARTBEAT_REQ (1053) được phép
        //    trước khi login thành công (tương đương C++ has_login = True)
        if (!ps.isLoggedIn() && msgId != 7056 && msgId != 1053) {
            log.warn("[gateway] Reject msgId={} — session not authenticated ws={}", msgId, ps.getWs().getId());
            analyticsHandler.track(ps, "MSG_REJECTED_NOT_AUTH", "SECURITY",
                    Map.of("msgId", msgId));
            return Mono.empty();
        }

        return registry.find(msgId)
                .map(h ->
                        // ── INJECT TOKEN: set vào FeignTokenHolder TRƯỚC khi handler chạy ──
                        // Mono.defer() chạy trên virtual thread (do .subscribeOn bên dưới),
                        // nên FeignTokenHolder (ThreadLocal) có giá trị đúng cho mọi feign call
                        // bên trong handler — dù handler có dùng FeignCall.withToken hay không.
                        Mono.defer(() -> {
                                    String token = ps.getSessionId();
                                    if (token != null && !token.isBlank()) {
                                        FeignTokenHolder.set(token);
                                    }
                                    // Track mỗi message được dispatch (bỏ qua heartbeat/time — quá nhiều, không có giá trị)
                                    if (msgId != MsgIds.CS_HEARTBEAT_REQ && msgId != MsgIds.CS_TIME_REQ) {
                                        analyticsHandler.track(ps, "MSG_DISPATCH", "GAMEPLAY",
                                                Map.of("msgId",   msgId,
                                                       "handler", h.getClass().getSimpleName()));
                                    }
                                    return h.handle(ps, msgId, payload);
                                })
                                .subscribeOn(feignScheduler)
                                .doFinally(sig -> FeignTokenHolder.clear())  // ← dọn ThreadLocal sau mỗi request
                                .timeout(HANDLER_TIMEOUT)
                                .onErrorResume(java.util.concurrent.TimeoutException.class, ex -> {
                                    log.error("[gateway] TIMEOUT msgId={} handler={} — downstream service có thể down",
                                            msgId, h.getClass().getSimpleName());
                                    // Track timeout để phát hiện service chết
                                    analyticsHandler.track(ps, "DISPATCH_TIMEOUT", "ERROR",
                                            Map.of("msgId",   msgId,
                                                   "handler", h.getClass().getSimpleName()));
                                    return Mono.empty();
                                })
                                .onErrorResume(ex -> {
                                    log.warn("[gateway] Handler {} msgId={} error: {}",
                                            h.getClass().getSimpleName(), msgId, ex.toString());
                                    // Track lỗi handler
                                    analyticsHandler.track(ps, "DISPATCH_ERROR", "ERROR",
                                            Map.of("msgId",   msgId,
                                                   "handler", h.getClass().getSimpleName(),
                                                   "error",   ex.getClass().getSimpleName(),
                                                   "msg",     ex.getMessage() != null ? ex.getMessage() : ""));
                                    return Mono.empty();
                                }))
                .orElseGet(() -> {
                    log.warn("Unknown MsgId {}", msgId);
                    analyticsHandler.track(ps, "MSG_UNKNOWN", "WARNING",
                            Map.of("msgId", msgId));
                    return Mono.empty();
                });
    }
}