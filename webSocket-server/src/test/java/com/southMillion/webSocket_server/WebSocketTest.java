package com.southMillion.webSocket_server;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebSocketTest {
    public static void main(String[] args) {
        try {
            String wsUrl = "ws://localhost:8080/webSocket-server/ws/game"; // Thay URL WebSocket server của bạn
            WebSocketClient client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Kết nối thành công!");
                    send("Hello server!"); // Test gửi message
                }
                @Override
                public void onMessage(String message) {
                    System.out.println("Nhận tin nhắn từ server: " + message);
                }
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Đã đóng kết nối. Reason: " + reason);
                }
                @Override
                public void onError(Exception ex) {
                    System.out.println("Lỗi: " + ex.getMessage());
                }
            };

            client.connect();

            // Đợi kết nối và nhận tin nhắn (tùy nhu cầu, có thể sleep hoặc dùng CountDownLatch)
            Thread.sleep(5000);

            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}