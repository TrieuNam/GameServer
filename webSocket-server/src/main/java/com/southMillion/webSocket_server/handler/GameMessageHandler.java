package com.southMillion.webSocket_server.handler;

import org.springframework.web.socket.WebSocketSession;

public interface GameMessageHandler {
    void handle(WebSocketSession session, byte[] payload);

    int getMsgId(); // Để auto đăng ký vào map
}
