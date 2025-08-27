package com.southMillion.webSocket_server.net;

import com.southMillion.webSocket_server.dto.PlayerSession;
import reactor.core.publisher.Mono;

public interface MessageHandler {
    /** Danh sách MsgId mà handler này xử lý. */
    int[] interests();

    /** Xử lý 1 gói vào (đã tách msgId + payload theo PacketCodec). */
    Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload);
}
