package com.southMillion.webSocket_server.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public WebSocketSession getSessionByUsername(String username) {
        return sessionMap.get(username);
    }

    public void bindSession(String username, WebSocketSession session) {
        sessionMap.put(username, session);
    }

    public void removeSession(String username) {
        sessionMap.remove(username);
    }
}
