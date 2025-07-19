package com.southMillion.webSocket_server.handler;

import org.springframework.web.socket.WebSocketSession;

public class ChatMessageHandler implements GameMessageHandler {
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
//        try {
//            MsgC PB_CSChat chat = PB_CSChatMessage.parseFrom(payload);
//            System.out.println("Chat from: " + chat.getSender());
//            // ... xử lý chat
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public int getMsgId() {
        return 8001;
    }
}