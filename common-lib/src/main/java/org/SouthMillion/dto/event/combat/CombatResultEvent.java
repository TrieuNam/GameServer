package org.SouthMillion.dto.event.combat;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Combat Result Event - Published after combat ends
 * Consumed by: statistics-service, achievement-service, quest-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombatResultEvent {
    private String eventId;
    private Long combatId;
    private Long roleId;
    
    // Combat info
    private String combatType;   // "PVE", "PVP", "BOSS", "ARENA", "TRIAL"
    private Boolean isVictory;
    private Integer duration;    // seconds
    
    // Enemy info
    private String enemyType;    // "MONSTER", "PLAYER", "BOSS"
    private Integer enemyId;
    private Integer enemyLevel;
    
    // Stats
    private Long totalDamage;
    private Long totalHealing;
    private Integer killCount;
    private Integer deathCount;
    private Integer comboMax;
    
    // Rewards
    private Long expGained;
    private List<ItemDrop> itemsDropped;
    
    private Instant completedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDrop {
        private Integer itemId;
        private Integer quantity;
        private Integer quality;  // Rarity
    }
}
