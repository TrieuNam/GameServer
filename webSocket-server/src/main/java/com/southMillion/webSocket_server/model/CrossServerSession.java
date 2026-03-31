package com.SouthMillion.webSocket_server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Cross-server session data model.
 * 
 * Stores player session information during cross-server transfers.
 * Serialized to Redis for session migration between servers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossServerSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Player's original role ID
     */
    private String roleId;
    
    /**
     * Player's user ID
     */
    private String userId;
    
    /**
     * Temporary cross-server UID
     */
    private Integer crossServerUid;
    
    /**
     * Origin server identifier
     */
    private String originServerId;
    
    /**
     * Target cross-server identifier
     */
    private String targetServerId;
    
    /**
     * Gateway hostname for connection
     */
    private String gatewayHost;
    
    /**
     * Gateway port for connection
     */
    private Integer gatewayPort;
    
    /**
     * Current scene ID
     */
    private Integer sceneId;
    
    /**
     * Last scene ID (before transfer)
     */
    private Integer lastSceneId;
    
    /**
     * Session creation timestamp (Unix epoch seconds)
     */
    private Long createdAt;
    
    /**
     * Session expiry timestamp (Unix epoch seconds)
     */
    private Long expiresAt;
    
    /**
     * Cryptographic session key for validation
     */
    private String sessionKey;
    
    /**
     * Player level (for eligibility validation)
     */
    private Integer playerLevel;
    
    /**
     * Session status: PENDING, ACTIVE, EXPIRED, RETURNED
     */
    private SessionStatus status;
    
    /**
     * Additional metadata (JSON string)
     */
    private String metadata;
    
    public enum SessionStatus {
        PENDING,    // Transfer initiated but not completed
        ACTIVE,     // Player is on cross-server
        EXPIRED,    // Session timed out
        RETURNED    // Player returned to origin
    }
    
    /**
     * Check if session is still valid
     */
    public boolean isValid() {
        long now = System.currentTimeMillis() / 1000;
        return status == SessionStatus.ACTIVE && expiresAt > now;
    }
    
    /**
     * Check if session has expired
     */
    public boolean isExpired() {
        long now = System.currentTimeMillis() / 1000;
        return expiresAt <= now;
    }
}
