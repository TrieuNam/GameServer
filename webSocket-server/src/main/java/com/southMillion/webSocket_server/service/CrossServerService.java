package com.SouthMillion.webSocket_server.service;

import com.SouthMillion.webSocket_server.model.CrossServerSession;

import java.util.Optional;

/**
 * Service for managing cross-server sessions and transfers.
 * 
 * Handles:
 * - Session creation and validation
 * - Cross-server UID generation
 * - Session key cryptography
 * - Redis-based session storage
 */
public interface CrossServerService {
    
    /**
     * Create a new cross-server session for a player.
     * 
     * @param roleId Player's role ID
     * @param userId Player's user ID
     * @param playerLevel Player's level (for eligibility check)
     * @param targetServerId Target cross-server identifier
     * @return Created session
     * @throws IllegalArgumentException if player is not eligible
     */
    CrossServerSession createSession(String roleId, String userId, Integer playerLevel, String targetServerId);
    
    /**
     * Get an existing cross-server session.
     * 
     * @param roleId Player's role ID
     * @return Optional of CrossServerSession
     */
    Optional<CrossServerSession> getSession(String roleId);
    
    /**
     * Activate a pending session (player has connected to cross-server).
     * 
     * @param roleId Player's role ID
     * @return true if activation successful
     */
    boolean activateSession(String roleId);
    
    /**
     * Mark session as returned to origin.
     * 
     * @param roleId Player's role ID
     * @return true if return successful
     */
    boolean returnToOrigin(String roleId);
    
    /**
     * Invalidate and delete a session.
     * 
     * @param roleId Player's role ID
     */
    void invalidateSession(String roleId);
    
    /**
     * Validate a session key for security.
     * 
     * @param roleId Player's role ID
     * @param sessionKey Session key to validate
     * @return true if valid
     */
    boolean validateSessionKey(String roleId, String sessionKey);
    
    /**
     * Check if player is eligible for cross-server transfer.
     * 
     * @param roleId Player's role ID
     * @param playerLevel Player's level
     * @return true if eligible
     */
    boolean isEligible(String roleId, Integer playerLevel);
    
    /**
     * Generate a unique cross-server UID for a player.
     * 
     * @param roleId Player's role ID
     * @return Cross-server UID
     */
    Integer generateCrossServerUid(String roleId);
    
    /**
     * Generate a cryptographic session key.
     * 
     * @param roleId Player's role ID
     * @return Session key (hex string)
     */
    String generateSessionKey(String roleId);
}
