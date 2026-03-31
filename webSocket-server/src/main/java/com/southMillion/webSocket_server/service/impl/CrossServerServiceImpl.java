package com.SouthMillion.webSocket_server.service.impl;

import com.SouthMillion.webSocket_server.model.CrossServerSession;
import com.SouthMillion.webSocket_server.service.CrossServerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of CrossServerService using Redis for session storage.
 * 
 * Sessions are stored with TTL for automatic cleanup.
 * Session keys use HMAC-SHA256 for cryptographic validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossServerServiceImpl implements CrossServerService {
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${cross-server.session.ttl-minutes:30}")
    private int sessionTtlMinutes;
    
    @Value("${cross-server.session.secret:change-me-in-production}")
    private String sessionSecret;
    
    @Value("${cross-server.min-level:50}")
    private int minLevel;
    
    @Value("${cross-server.gateway.host:localhost}")
    private String gatewayHost;
    
    @Value("${cross-server.gateway.port:9000}")
    private int gatewayPort;
    
    @Value("${cross-server.origin-server-id:server-1}")
    private String originServerId;
    
    private static final String SESSION_KEY_PREFIX = "cross:session:";
    private static final String UID_COUNTER_KEY = "cross:uid:counter";
    
    @Override
    public CrossServerSession createSession(String roleId, String userId, Integer playerLevel, String targetServerId) {
        if (!isEligible(roleId, playerLevel)) {
            throw new IllegalArgumentException("Player not eligible for cross-server (level < " + minLevel + ")");
        }
        
        // Check if player already has an active session
        Optional<CrossServerSession> existing = getSession(roleId);
        if (existing.isPresent() && existing.get().isValid()) {
            log.warn("[CrossServer] Player {} already has active session", roleId);
            return existing.get();
        }
        
        long now = System.currentTimeMillis() / 1000;
        long expiresAt = now + (sessionTtlMinutes * 60L);
        
        CrossServerSession session = CrossServerSession.builder()
                .roleId(roleId)
                .userId(userId)
                .crossServerUid(generateCrossServerUid(roleId))
                .originServerId(originServerId)
                .targetServerId(targetServerId)
                .gatewayHost(gatewayHost)
                .gatewayPort(gatewayPort)
                .sceneId(1001)  // Default cross-server scene
                .lastSceneId(1000)  // Default origin scene
                .createdAt(now)
                .expiresAt(expiresAt)
                .sessionKey(generateSessionKey(roleId))
                .playerLevel(playerLevel)
                .status(CrossServerSession.SessionStatus.PENDING)
                .build();
        
        try {
            String json = objectMapper.writeValueAsString(session);
            String key = SESSION_KEY_PREFIX + roleId;
            redisTemplate.opsForValue().set(key, json, sessionTtlMinutes, TimeUnit.MINUTES);
            log.info("[CrossServer] Created session for roleId={}, crossUid={}, expires={}",
                    roleId, session.getCrossServerUid(), expiresAt);
            return session;
        } catch (Exception e) {
            log.error("[CrossServer] Failed to create session for roleId={}", roleId, e);
            throw new RuntimeException("Failed to create cross-server session", e);
        }
    }
    
    @Override
    public Optional<CrossServerSession> getSession(String roleId) {
        try {
            String key = SESSION_KEY_PREFIX + roleId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            
            CrossServerSession session = objectMapper.readValue(json, CrossServerSession.class);
            
            // Check expiry
            if (session.isExpired()) {
                log.warn("[CrossServer] Session expired for roleId={}", roleId);
                invalidateSession(roleId);
                return Optional.empty();
            }
            
            return Optional.of(session);
        } catch (Exception e) {
            log.error("[CrossServer] Failed to get session for roleId={}", roleId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean activateSession(String roleId) {
        Optional<CrossServerSession> sessionOpt = getSession(roleId);
        if (sessionOpt.isEmpty()) {
            log.warn("[CrossServer] Cannot activate session - not found: roleId={}", roleId);
            return false;
        }
        
        CrossServerSession session = sessionOpt.get();
        session.setStatus(CrossServerSession.SessionStatus.ACTIVE);
        
        try {
            String json = objectMapper.writeValueAsString(session);
            String key = SESSION_KEY_PREFIX + roleId;
            redisTemplate.opsForValue().set(key, json, sessionTtlMinutes, TimeUnit.MINUTES);
            log.info("[CrossServer] Activated session for roleId={}", roleId);
            return true;
        } catch (Exception e) {
            log.error("[CrossServer] Failed to activate session for roleId={}", roleId, e);
            return false;
        }
    }
    
    @Override
    public boolean returnToOrigin(String roleId) {
        Optional<CrossServerSession> sessionOpt = getSession(roleId);
        if (sessionOpt.isEmpty()) {
            log.warn("[CrossServer] Cannot return - session not found: roleId={}", roleId);
            return false;
        }
        
        CrossServerSession session = sessionOpt.get();
        session.setStatus(CrossServerSession.SessionStatus.RETURNED);
        
        try {
            String json = objectMapper.writeValueAsString(session);
            String key = SESSION_KEY_PREFIX + roleId;
            // Keep session for 5 minutes after return for logging/debugging
            redisTemplate.opsForValue().set(key, json, 5, TimeUnit.MINUTES);
            log.info("[CrossServer] Player returned to origin: roleId={}", roleId);
            return true;
        } catch (Exception e) {
            log.error("[CrossServer] Failed to mark return for roleId={}", roleId, e);
            return false;
        }
    }
    
    @Override
    public void invalidateSession(String roleId) {
        String key = SESSION_KEY_PREFIX + roleId;
        redisTemplate.delete(key);
        log.info("[CrossServer] Invalidated session for roleId={}", roleId);
    }
    
    @Override
    public boolean validateSessionKey(String roleId, String sessionKey) {
        Optional<CrossServerSession> sessionOpt = getSession(roleId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        String storedKey = sessionOpt.get().getSessionKey();
        boolean valid = storedKey != null && storedKey.equals(sessionKey);
        
        if (!valid) {
            log.warn("[CrossServer] Invalid session key for roleId={}", roleId);
        }
        
        return valid;
    }
    
    @Override
    public boolean isEligible(String roleId, Integer playerLevel) {
        if (playerLevel == null || playerLevel < minLevel) {
            log.debug("[CrossServer] Player {} not eligible - level {} < {}", roleId, playerLevel, minLevel);
            return false;
        }
        
        // Additional eligibility checks can be added here:
        // - No active trades
        // - No active battles
        // - No pending transactions
        
        return true;
    }
    
    @Override
    public Integer generateCrossServerUid(String roleId) {
        // Use Redis counter for unique UID generation
        Long counter = redisTemplate.opsForValue().increment(UID_COUNTER_KEY);
        if (counter == null) {
            counter = 1L;
        }
        
        // Generate UID in range 100000-999999 (6 digits)
        int uid = (int) (100000 + (counter % 900000));
        log.debug("[CrossServer] Generated cross-server UID {} for roleId={}", uid, roleId);
        return uid;
    }
    
    @Override
    public String generateSessionKey(String roleId) {
        try {
            // Use HMAC-SHA256 for better security
            long timestamp = System.currentTimeMillis();
            String data = roleId + ":" + timestamp;
            
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    sessionSecret.getBytes(StandardCharsets.UTF_8), 
                    "HmacSHA256"
            );
            hmac.init(secretKey);
            
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("[CrossServer] Failed to generate session key with HMAC, using fallback", e);
            // Fallback to SHA-256 if HMAC fails
            try {
                String data = roleId + ":" + System.currentTimeMillis() + ":" + sessionSecret;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
                
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (Exception ex) {
                log.error("[CrossServer] All key generation methods failed, using UUID", ex);
                return UUID.randomUUID().toString();
            }
        }
    }
}
