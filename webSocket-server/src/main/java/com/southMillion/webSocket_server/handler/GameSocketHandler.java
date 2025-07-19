package com.southMillion.webSocket_server.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class GameSocketHandler extends BinaryWebSocketHandler {
    private final Map<Integer, GameMessageHandler> handlerMap = new HashMap<>();

    @Autowired
    public GameSocketHandler(List<GameMessageHandler> handlers) {
        for (GameMessageHandler handler : handlers) {
            handlerMap.put(handler.getMsgId(), handler);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer buffer = message.getPayload();
        buffer.order(ByteOrder.BIG_ENDIAN);
        int length = buffer.getInt();
        int msgId = buffer.getInt();
        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);

        GameMessageHandler handler = handlerMap.get(msgId);
        if (handler != null) {
            handler.handle(session, payload);
        } else {
            System.out.println("Unknown msgId: " + msgId);
        }
    }
}