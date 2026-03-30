package com.SouthMillion.angel_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when an angel levels up
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AngelLevelUpEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer angelId;
    private Integer angelIndex;
    private Integer oldLevel;
    private Integer newLevel;
    private Long expGained;
    private Long timestamp = System.currentTimeMillis();
    private String source = "angel-service";
    
    public AngelLevelUpEvent(Long userId, Integer angelId, Integer angelIndex, 
                            Integer oldLevel, Integer newLevel, Long expGained) {
        this.userId = userId;
        this.angelId = angelId;
        this.angelIndex = angelIndex;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.expGained = expGained;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
