package com.SouthMillion.worlds_ervice.service;

import com.SouthMillion.worlds_ervice.client.BagFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Scene Management Service - Using Redis for distributed state
 * Handles player entering/leaving scenes, position updates, and visibility
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneManagementService {

    private static final String SCENE_PLAYERS_KEY = "scene:players:";      // scene:{sceneId} -> Set<roleId>
    private static final String PLAYER_SCENE_KEY = "player:scene:";        // player:scene:{roleId} -> sceneId
    private static final String PLAYER_POSITION_KEY = "player:position:";  // player:position:{roleId} -> PlayerPosition
    private static final String SCENE_ITEMS_KEY = "scene:items:";          // scene:items:{sceneId} -> Set<itemUid>
    private static final long POSITION_TTL_MINUTES = 30; // Auto-cleanup inactive players

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final BagFeignClient bagFeignClient;

    /**
     * Enter scene
     */
    public Map<String, Object> enterScene(Long roleId, Integer sceneId, Integer enterType, 
                                          Float x, Float y, Float z) {
        log.info("[SceneManagement] Enter scene - roleId: {}, sceneId: {}, enterType: {}", 
                roleId, sceneId, enterType);

        // Leave current scene if any
        Integer currentScene = getPlayerScene(roleId);
        if (currentScene != null && !currentScene.equals(sceneId)) {
            leaveScene(roleId, currentScene, 1); // Normal leave
        }

        // Add player to scene
        String scenePlayersKey = SCENE_PLAYERS_KEY + sceneId;
        redisTemplate.opsForSet().add(scenePlayersKey, roleId);
        redisTemplate.expire(scenePlayersKey, POSITION_TTL_MINUTES, TimeUnit.MINUTES);

        // Track player's current scene
        String playerSceneKey = PLAYER_SCENE_KEY + roleId;
        redisTemplate.opsForValue().set(playerSceneKey, sceneId, POSITION_TTL_MINUTES, TimeUnit.MINUTES);

        // Set initial position
        PlayerPosition position = new PlayerPosition();
        position.x = x != null ? x : getDefaultSpawnX(sceneId);
        position.y = y != null ? y : 0.0f;
        position.z = z != null ? z : getDefaultSpawnZ(sceneId);
        position.dirX = 0.0f;
        position.dirY = 0.0f;
        position.dirZ = 0.0f;
        position.sceneId = sceneId;
        position.timestamp = System.currentTimeMillis();
        
        String positionKey = PLAYER_POSITION_KEY + roleId;
        redisTemplate.opsForValue().set(positionKey, position, POSITION_TTL_MINUTES, TimeUnit.MINUTES);

        // Get nearby players
        List<Long> nearbyPlayers = getNearbyPlayers(roleId, sceneId, position.x, position.z);

        // Get total players in scene
        Long totalPlayers = redisTemplate.opsForSet().size(scenePlayersKey);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sceneId", sceneId);
        result.put("roleId", roleId);
        result.put("posX", position.x);
        result.put("posY", position.y);
        result.put("posZ", position.z);
        result.put("nearbyPlayers", nearbyPlayers);
        result.put("totalPlayers", totalPlayers != null ? totalPlayers : 0);

        log.info("[SceneManagement] Enter scene success - roleId: {}, sceneId: {}, nearbyPlayers: {}", 
                roleId, sceneId, nearbyPlayers.size());

        return result;
    }

    /**
     * Leave scene
     */
    public Map<String, Object> leaveScene(Long roleId, Integer sceneId, Integer leaveType) {
        log.info("[SceneManagement] Leave scene - roleId: {}, sceneId: {}, leaveType: {}", 
                roleId, sceneId, leaveType);

        // Remove from scene
        String scenePlayersKey = SCENE_PLAYERS_KEY + sceneId;
        redisTemplate.opsForSet().remove(scenePlayersKey, roleId);

        // Remove player tracking
        String playerSceneKey = PLAYER_SCENE_KEY + roleId;
        String positionKey = PLAYER_POSITION_KEY + roleId;
        redisTemplate.delete(playerSceneKey);
        redisTemplate.delete(positionKey);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("roleId", roleId);
        result.put("sceneId", sceneId);
        result.put("leaveType", leaveType);

        log.info("[SceneManagement] Leave scene success - roleId: {}, sceneId: {}", roleId, sceneId);

        return result;
    }

    /**
     * Update player position
     */
    public Map<String, Object> updatePosition(Long roleId, Float x, Float y, Float z, 
                                               Float dirX, Float dirY, Float dirZ) {
        String positionKey = PLAYER_POSITION_KEY + roleId;
        Object posObj = redisTemplate.opsForValue().get(positionKey);
        
        if (posObj == null) {
            log.warn("[SceneManagement] Player not in any scene - roleId: {}", roleId);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Player not in scene");
            return error;
        }

        PlayerPosition position = (PlayerPosition) posObj;

        // Anti-cheat: Validate movement distance
        float distance = calculateDistance(position.x, position.z, x, z);
        long timeDiff = System.currentTimeMillis() - position.timestamp;
        float maxSpeed = 20.0f; // Max units per second
        float maxDistance = (maxSpeed * timeDiff) / 1000.0f + 10.0f; // +10 tolerance

        if (distance > maxDistance) {
            log.warn("[SceneManagement] Suspicious movement detected - roleId: {}, distance: {}, maxAllowed: {}", 
                    roleId, distance, maxDistance);
            // Could flag for further investigation, but allow for now
        }

        // Update position
        position.x = x;
        position.y = y;
        position.z = z;
        position.dirX = dirX != null ? dirX : 0.0f;
        position.dirY = dirY != null ? dirY : 0.0f;
        position.dirZ = dirZ != null ? dirZ : 0.0f;
        position.timestamp = System.currentTimeMillis();
        
        // Save back to Redis
        redisTemplate.opsForValue().set(positionKey, position, POSITION_TTL_MINUTES, TimeUnit.MINUTES);

        // Get nearby players for AOI
        List<Long> nearbyPlayers = getNearbyPlayers(roleId, position.sceneId, x, z);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("roleId", roleId);
        result.put("x", x);
        result.put("y", y);
        result.put("z", z);
        result.put("nearbyPlayers", nearbyPlayers);

        return result;
    }

    /**
     * Pickup item from scene
     */
    public Map<String, Object> pickupItem(Long roleId, Long itemUid) {
        String positionKey = PLAYER_POSITION_KEY + roleId;
        Object posObj = redisTemplate.opsForValue().get(positionKey);
        
        if (posObj == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Player not in scene");
            return error;
        }

        PlayerPosition position = (PlayerPosition) posObj;
        Integer sceneId = position.sceneId;
        String sceneItemsKey = SCENE_ITEMS_KEY + sceneId;
        
        Boolean isMember = redisTemplate.opsForSet().isMember(sceneItemsKey, itemUid);
        if (!Boolean.TRUE.equals(isMember)) {
            log.warn("[SceneManagement] Item not found - roleId: {}, itemUid: {}", roleId, itemUid);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Item not found or already picked up");
            return error;
        }

        // Remove item from scene
        redisTemplate.opsForSet().remove(sceneItemsKey, itemUid);

        // Derive itemId from itemUid (scene items encode itemId in lower 4 digits)
        int itemId = (int)(itemUid % 10000);
        int count = 1;

        // Grant item to player bag via bag-service
        try {
            BagDTOs.GrantReq grantReq = new BagDTOs.GrantReq();
            grantReq.setRoleId(String.valueOf(roleId));
            grantReq.setItems(java.util.List.of(
                BagDTOs.GrantItem.builder().itemId(itemId).num(count).build()
            ));
            grantReq.setReason("scene_pickup");
            bagFeignClient.grantItems(grantReq);
        } catch (Exception e) {
            log.error("[SceneManagement] Failed to grant item to bag - roleId: {}, itemId: {}, error: {}",
                    roleId, itemId, e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("roleId", roleId);
        result.put("itemUid", itemUid);
        result.put("itemId", itemId);
        result.put("count", count);

        log.info("[SceneManagement] Pickup item success - roleId: {}, itemUid: {}, itemId: {}",
                roleId, itemUid, itemId);

        return result;
    }

    /**
     * Interact with NPC
     */
    public Map<String, Object> interactNpc(Long roleId, Integer npcId, Integer interactType) {
        String positionKey = PLAYER_POSITION_KEY + roleId;
        Object posObj = redisTemplate.opsForValue().get(positionKey);
        
        if (posObj == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Player not in scene");
            return error;
        }

        log.info("[SceneManagement] Interact NPC - roleId: {}, npcId: {}, interactType: {}", 
                roleId, npcId, interactType);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("roleId", roleId);
        result.put("npcId", npcId);
        result.put("interactType", interactType);
        
        // Interaction types: 1-dialog, 2-shop, 3-task
        switch (interactType) {
            case 1: // Dialog
                result.put("dialogId", 1000 + npcId);
                break;
            case 2: // Shop
                result.put("shopId", npcId);
                result.put("shopType", 1);
                break;
            case 3: // Task
                result.put("taskListId", npcId);
                break;
            default:
                result.put("action", "generic_interact");
        }

        log.info("[SceneManagement] Interact NPC success - roleId: {}, npcId: {}", roleId, npcId);

        return result;
    }

    /**
     * Get scene information
     */
    public Map<String, Object> getSceneInfo(Integer sceneId) {
        String scenePlayersKey = SCENE_PLAYERS_KEY + sceneId;
        String sceneItemsKey = SCENE_ITEMS_KEY + sceneId;
        
        Long playerCount = redisTemplate.opsForSet().size(scenePlayersKey);
        Long itemCount = redisTemplate.opsForSet().size(sceneItemsKey);

        Map<String, Object> info = new HashMap<>();
        info.put("sceneId", sceneId);
        info.put("playerCount", playerCount != null ? playerCount : 0);
        info.put("itemCount", itemCount != null ? itemCount : 0);
        info.put("sceneName", getSceneName(sceneId));
        info.put("sceneType", getSceneType(sceneId));

        return info;
    }

    /**
     * Get nearby players within AOI radius
     */
    private List<Long> getNearbyPlayers(Long roleId, Integer sceneId, Float x, Float z) {
        String scenePlayersKey = SCENE_PLAYERS_KEY + sceneId;
        Set<Object> playersInScene = redisTemplate.opsForSet().members(scenePlayersKey);
        
        if (playersInScene == null || playersInScene.isEmpty()) {
            return Collections.emptyList();
        }

        float aoiRadius = 50.0f; // Units
        List<Long> nearby = new ArrayList<>();

        for (Object playerObj : playersInScene) {
            if (!(playerObj instanceof Long otherRoleId) || otherRoleId.equals(roleId)) {
                continue; // Skip self
            }

            String otherPositionKey = PLAYER_POSITION_KEY + otherRoleId;
            Object otherPosObj = redisTemplate.opsForValue().get(otherPositionKey);
            
            if (otherPosObj instanceof PlayerPosition otherPos) {
                float distance = calculateDistance(x, z, otherPos.x, otherPos.z);
                if (distance <= aoiRadius) {
                    nearby.add(otherRoleId);
                }
            }
        }

        return nearby;
    }

    /**
     * Calculate 2D distance
     */
    private float calculateDistance(float x1, float z1, float x2, float z2) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Get default spawn position X for scene
     */
    private float getDefaultSpawnX(Integer sceneId) {
        // Mock implementation - in real game, load from config
        return switch (sceneId % 5) {
            case 1 -> 100.0f;
            case 2 -> 200.0f;
            case 3 -> 150.0f;
            case 4 -> 250.0f;
            default -> 100.0f;
        };
    }

    /**
     * Get default spawn position Z for scene
     */
    private float getDefaultSpawnZ(Integer sceneId) {
        return switch (sceneId % 5) {
            case 1 -> 100.0f;
            case 2 -> 150.0f;
            case 3 -> 200.0f;
            case 4 -> 180.0f;
            default -> 100.0f;
        };
    }

    /**
     * Get scene name
     */
    private String getSceneName(Integer sceneId) {
        return "Scene_" + sceneId; // Mock - load from config in real implementation
    }

    /**
     * Get scene type
     */
    private int getSceneType(Integer sceneId) {
        // 1-city, 2-field, 3-dungeon, 4-pvp
        return (sceneId % 4) + 1;
    }

    /**
     * Player position data class
     */
    @Data
    public static class PlayerPosition {
        private float x;
        private float y;
        private float z;
        private float dirX;
        private float dirY;
        private float dirZ;
        private Integer sceneId;
        private long timestamp;
    }

    /**
     * Drop item in scene (for testing)
     */
    public void dropItem(Integer sceneId, Long itemUid) {
        String sceneItemsKey = SCENE_ITEMS_KEY + sceneId;
        redisTemplate.opsForSet().add(sceneItemsKey, itemUid);
        redisTemplate.expire(sceneItemsKey, POSITION_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Get player current scene
     */
    public Integer getPlayerScene(Long roleId) {
        String playerSceneKey = PLAYER_SCENE_KEY + roleId;
        Object sceneIdObj = redisTemplate.opsForValue().get(playerSceneKey);
        return sceneIdObj instanceof Integer ? (Integer) sceneIdObj : null;
    }

    /**
     * Get players in scene
     */
    public Set<Long> getPlayersInScene(Integer sceneId) {
        String scenePlayersKey = SCENE_PLAYERS_KEY + sceneId;
        Set<Object> players = redisTemplate.opsForSet().members(scenePlayersKey);
        
        if (players == null) {
            return Collections.emptySet();
        }
        
        Set<Long> result = new HashSet<>();
        for (Object obj : players) {
            if (obj instanceof Long roleId) {
                result.add(roleId);
            }
        }
        return result;
    }
}
