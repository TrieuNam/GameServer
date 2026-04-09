package com.SouthMillion.battleserver_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Standardized Combat Event DTO for Kafka Publishing
 *
 * This event is published in dual-perspective:
 * - Once for the attacker's viewpoint
 * - Once for the defender's viewpoint
 *
 * Version: 1.0
 * Consumers: analytics-service, leaderboard-service, achievement-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatEvent {

    // Event metadata
    private String eventType;        // "COMBAT_RESULT"
    private String eventVersion;     // "1.0"
    private long timestamp;          // Event timestamp (ms)
    private String combatId;         // Unique combat identifier (UUID)
    private String sessionId;        // Session ID (if applicable)
    private String combatType;       // PVP, PVE, ARENA, TRIAL, DUNGEON, BOSS
    private int duration;            // Combat duration in milliseconds

    // Combatant information
    private Combatant attacker;
    private Combatant defender;

    // Combat result
    private CombatResult result;

    // Additional metadata
    private Map<String, Object> metadata;

    // Perspective for this event instance
    private String perspective;      // "ATTACKER" or "DEFENDER"
    private boolean isWinner;        // Is this perspective the winner?

    /**
     * Combatant data structure
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Combatant {
        private Long roleId;
        private String name;
        private Integer level;
        private Long power;          // Combat power rating
        private Long damage;         // Total damage dealt
        private Long damageTaken;    // Total damage taken
        private Long healing;        // Total healing done
        private Integer finalHp;     // Final HP after combat
        private boolean survived;    // Did combatant survive?
    }

    /**
     * Combat result data structure
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombatResult {
        private Long winnerId;
        private String winnerSide;   // "ATTACKER", "DEFENDER", or "DRAW"
        private Integer totalRounds;
        private Long xpGained;       // XP gained by this perspective
        private Long goldGained;     // Gold gained by this perspective
        private Integer comboMax;    // Maximum combo achieved
    }
}
