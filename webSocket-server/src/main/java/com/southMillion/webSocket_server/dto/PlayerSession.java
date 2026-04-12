package com.SouthMillion.webSocket_server.dto;

import com.SouthMillion.webSocket_server.net.PacketCodec;
import org.SouthMillion.proto.Msgworld.Msgworld;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

public class PlayerSession {
    private final WebSocketSession ws;
    private final Sinks.Many<byte[]> outbound; // encoded frames to send
    private String sessionId; // Bearer token from handshake/payload
    private String analyticsSessionId; // Canonical sid used for analytics/logging
    private String userId;
    private Long roleId;
    private String roleName;
    private String username;
    private Integer lastKnownRoleLevel;
    private Long lastKnownRoleExp;

    /** True after successful login (CS:7056 validated). Guards all non-login messages. */
    private boolean loggedIn;
    private Integer currentSceneId;  // Current scene/map ID for world handler
    private Long lastMoveTimestamp;  // For world movement anti-cheat validation
    private Msgworld.PB_Position lastPosition; // Last confirmed world position

    public PlayerSession(WebSocketSession ws, Sinks.Many<byte[]> outbound, String sessionId,
                         String analyticsSessionId, String userId, Long roleId,
                         String roleName, String username,
                         Integer lastKnownRoleLevel, Long lastKnownRoleExp,
                         boolean loggedIn, Integer currentSceneId, Long lastMoveTimestamp,
                         Msgworld.PB_Position lastPosition) {
        this.ws = ws;
        this.outbound = outbound;
        this.sessionId = sessionId;
        this.analyticsSessionId = analyticsSessionId;
        this.userId = userId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.username = username;
        this.lastKnownRoleLevel = lastKnownRoleLevel;
        this.lastKnownRoleExp = lastKnownRoleExp;
        this.loggedIn = loggedIn;
        this.currentSceneId = currentSceneId;
        this.lastMoveTimestamp = lastMoveTimestamp;
        this.lastPosition = lastPosition;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private WebSocketSession ws;
        private Sinks.Many<byte[]> outbound;
        private String sessionId;
        private String analyticsSessionId;
        private String userId;
        private Long roleId;
        private String roleName;
        private String username;
        private Integer lastKnownRoleLevel;
        private Long lastKnownRoleExp;
        private boolean loggedIn;
        private Integer currentSceneId;
        private Long lastMoveTimestamp;
        private Msgworld.PB_Position lastPosition;

        public Builder ws(WebSocketSession ws) { this.ws = ws; return this; }
        public Builder outbound(Sinks.Many<byte[]> outbound) { this.outbound = outbound; return this; }
        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder analyticsSessionId(String analyticsSessionId) { this.analyticsSessionId = analyticsSessionId; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder roleId(Long roleId) { this.roleId = roleId; return this; }
        public Builder roleName(String roleName) { this.roleName = roleName; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder lastKnownRoleLevel(Integer lastKnownRoleLevel) { this.lastKnownRoleLevel = lastKnownRoleLevel; return this; }
        public Builder lastKnownRoleExp(Long lastKnownRoleExp) { this.lastKnownRoleExp = lastKnownRoleExp; return this; }
        public Builder loggedIn(boolean loggedIn) { this.loggedIn = loggedIn; return this; }
        public Builder currentSceneId(Integer currentSceneId) { this.currentSceneId = currentSceneId; return this; }
        public Builder lastMoveTimestamp(Long lastMoveTimestamp) { this.lastMoveTimestamp = lastMoveTimestamp; return this; }
        public Builder lastPosition(Msgworld.PB_Position lastPosition) { this.lastPosition = lastPosition; return this; }

        public PlayerSession build() {
            return new PlayerSession(ws, outbound, sessionId, analyticsSessionId, userId, roleId,
                    roleName, username, lastKnownRoleLevel, lastKnownRoleExp,
                    loggedIn, currentSceneId, lastMoveTimestamp, lastPosition);
        }
    }

    public WebSocketSession getWs() { return ws; }
    public Sinks.Many<byte[]> getOutbound() { return outbound; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAnalyticsSessionId() { return analyticsSessionId; }
    public void setAnalyticsSessionId(String analyticsSessionId) { this.analyticsSessionId = analyticsSessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getLastKnownRoleLevel() { return lastKnownRoleLevel; }
    public void setLastKnownRoleLevel(Integer lastKnownRoleLevel) { this.lastKnownRoleLevel = lastKnownRoleLevel; }
    public Long getLastKnownRoleExp() { return lastKnownRoleExp; }
    public void setLastKnownRoleExp(Long lastKnownRoleExp) { this.lastKnownRoleExp = lastKnownRoleExp; }
    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }
    public Integer getCurrentSceneId() { return currentSceneId; }
    public void setCurrentSceneId(Integer currentSceneId) { this.currentSceneId = currentSceneId; }
    public Long getLastMoveTimestamp() { return lastMoveTimestamp; }
    public void setLastMoveTimestamp(Long lastMoveTimestamp) { this.lastMoveTimestamp = lastMoveTimestamp; }
    public Msgworld.PB_Position getLastPosition() { return lastPosition; }
    public void setLastPosition(Msgworld.PB_Position lastPosition) { this.lastPosition = lastPosition; }

    public void sendBinary(byte[] frame) {
        outbound.tryEmitNext(frame);
    }

    /**
     * Send a message with msgId and payload
     */
    public void send(int msgId, byte[] payload) {
        byte[] frame = PacketCodec.encode(msgId, payload);
        outbound.tryEmitNext(frame);
    }
}