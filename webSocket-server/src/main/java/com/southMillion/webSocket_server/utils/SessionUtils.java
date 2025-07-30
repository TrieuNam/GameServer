package com.southMillion.webSocket_server.utils;

import org.springframework.web.socket.WebSocketSession;

public class SessionUtils {
    private static final String KEY_ROLE_ID = "roleId";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_SESSION_ID = "sessionId";

    // Lưu roleId vào session
    public static void setRoleId(WebSocketSession session, Integer roleId) {
        session.getAttributes().put(KEY_ROLE_ID, roleId);
    }

    // Lấy roleId từ session
    public static Integer getRoleId(WebSocketSession session) {
        Object v = session.getAttributes().get(KEY_ROLE_ID);
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof String) return Integer.valueOf((String) v);
        return null;
    }

    // Lưu userId (platUserName) nếu cần
    public static void setUserId(WebSocketSession session, String userId) {
        session.getAttributes().put(KEY_USER_ID, userId);
    }

    public static String getUserId(WebSocketSession session) {
        Object v = session.getAttributes().get(KEY_USER_ID);
        return v != null ? v.toString() : null;
    }

    public static void setSessionId(WebSocketSession session, String sessionId) {
        session.getAttributes().put(KEY_SESSION_ID, sessionId);
    }
    public static String getSessionId(WebSocketSession session) {
        Object v = session.getAttributes().get(KEY_SESSION_ID);
        return v != null ? v.toString() : null;
    }

}
