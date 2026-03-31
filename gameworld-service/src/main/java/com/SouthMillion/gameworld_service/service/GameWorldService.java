package com.SouthMillion.gameworld_service.service;

import com.SouthMillion.gameworld_service.dto.NearbyPlayersResponse;
import com.SouthMillion.gameworld_service.dto.PlayerPosition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GameWorld Service - Manages player positions and world state using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameWorldService {

    private static final String POSITION_KEY_PREFIX = "gameworld:position:";
    private static final String MAP_PLAYERS_KEY_PREFIX = "gameworld:map:";
    private static final long POSITION_TTL_MINUTES = 30; // Auto-expire inactive players

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Update player position
     */
    public PlayerPosition updatePosition(PlayerPosition position) {
        position.setTimestamp(System.currentTimeMillis());
        
        String positionKey = POSITION_KEY_PREFIX + position.getPlayerId();
        String mapKey = MAP_PLAYERS_KEY_PREFIX + position.getMapId();
        
        try {
            // Store player position with TTL
            redisTemplate.opsForValue().set(positionKey, position, POSITION_TTL_MINUTES, TimeUnit.MINUTES);
            
            // Add player to map index
            redisTemplate.opsForSet().add(mapKey, position.getPlayerId());
            redisTemplate.expire(mapKey, POSITION_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.debug("Updated position for player {}: ({}, {})", 
                     position.getPlayerId(), position.getX(), position.getY());
        } catch (Exception e) {
            log.error("Failed to update position for player {}", position.getPlayerId(), e);
        }
        
        return position;
    }

    /**
     * Get player position
     */
    public Optional<PlayerPosition> getPosition(Long playerId) {
        String positionKey = POSITION_KEY_PREFIX + playerId;
        try {
            Object position = redisTemplate.opsForValue().get(positionKey);
            if (position instanceof PlayerPosition) {
                return Optional.of((PlayerPosition) position);
            }
        } catch (Exception e) {
            log.error("Failed to get position for player {}", playerId, e);
        }
        return Optional.empty();
    }

    /**
     * Get nearby players within radius
     */
    public NearbyPlayersResponse getNearbyPlayers(Long playerId, Integer mapId, Float radius) {
        Optional<PlayerPosition> requesterPosOpt = getPosition(playerId);
        
        if (requesterPosOpt.isEmpty()) {
            return NearbyPlayersResponse.builder()
                    .requesterId(playerId)
                    .count(0)
                    .players(Collections.emptyList())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        PlayerPosition requesterPos = requesterPosOpt.get();
        List<PlayerPosition> nearbyPlayers = getPlayersOnMap(mapId).stream()
                .filter(p -> !p.getPlayerId().equals(playerId)) // Exclude self
                .filter(p -> calculateDistance(requesterPos, p) <= radius)
                .collect(Collectors.toList());

        return NearbyPlayersResponse.builder()
                .requesterId(playerId)
                .count(nearbyPlayers.size())
                .players(nearbyPlayers)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Batch update positions
     */
    public List<PlayerPosition> batchUpdatePositions(List<PlayerPosition> positions) {
        long now = System.currentTimeMillis();
        positions.forEach(pos -> {
            pos.setTimestamp(now);
            updatePosition(pos);
        });
        log.debug("Batch updated {} positions", positions.size());
        return positions;
    }

    /**
     * Remove player from world (disconnect)
     */
    public boolean removePlayer(Long playerId) {
        try {
            String positionKey = POSITION_KEY_PREFIX + playerId;
            
            // Get player position to find map
            Optional<PlayerPosition> posOpt = getPosition(playerId);
            if (posOpt.isPresent()) {
                Integer mapId = posOpt.get().getMapId();
                String mapKey = MAP_PLAYERS_KEY_PREFIX + mapId;
                
                // Remove from map index
                redisTemplate.opsForSet().remove(mapKey, playerId);
            }
            
            // Remove position
            Boolean deleted = redisTemplate.delete(positionKey);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Removed player {} from world", playerId);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to remove player {}", playerId, e);
        }
        return false;
    }

    /**
     * Get all players on a map
     */
    public List<PlayerPosition> getPlayersOnMap(Integer mapId) {
        try {
            String mapKey = MAP_PLAYERS_KEY_PREFIX + mapId;
            Set<Object> playerIds = redisTemplate.opsForSet().members(mapKey);
            
            if (playerIds == null || playerIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<PlayerPosition> positions = new ArrayList<>();
            for (Object playerIdObj : playerIds) {
                if (playerIdObj instanceof Long playerId) {
                    getPosition(playerId).ifPresent(positions::add);
                }
            }
            
            return positions;
        } catch (Exception e) {
            log.error("Failed to get players on map {}", mapId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Calculate distance between two positions
     */
    private float calculateDistance(PlayerPosition p1, PlayerPosition p2) {
        float dx = p1.getX() - p2.getX();
        float dy = p1.getY() - p2.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Validate position coordinates
     */
    public boolean validatePosition(PlayerPosition position) {
        return position != null
                && position.getPlayerId() != null
                && position.getMapId() != null
                && position.getX() != null
                && position.getY() != null
                && position.getX() >= 0 && position.getX() <= 10000
                && position.getY() >= 0 && position.getY() <= 10000;
    }
}
