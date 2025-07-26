package com.southMillion.webSocket_server.utils;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class websocketSendResponse {

    public static void sendResponse(WebSocketSession session, int msgId, byte[] payload) {
        try {
            int length = 4 + 4 + payload.length;
            ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(length);
            buffer.putInt(msgId);
            buffer.put(payload);
            buffer.flip();

            session.sendMessage(new BinaryMessage(buffer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Integer getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("uid"); // hoặc "userId"
        if (userId instanceof Integer) return (Integer) userId;
        if (userId instanceof Long) return ((Long) userId).intValue();
        if (userId instanceof String) return Integer.valueOf((String) userId);
        throw new RuntimeException("UserId not found in WebSocket session!");
    }
}
