package com.southMillion.webSocket_server.utils;

import org.springframework.web.socket.WebSocketSession;

public class WebSocketSessionHolder {
    private static final ThreadLocal<WebSocketSession> context = new ThreadLocal<>();

    public static void setCurrentSession(WebSocketSession session) {
        context.set(session);
    }

    public static WebSocketSession getCurrentSession() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}