package com.SouthMillion.pet_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pet Level Up Event
 * Published when a pet gains levels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetLevelUpEvent {
    
    /**
     * Unique event ID
     */
    private String eventId;
    
    /**
     * Owner role ID
     */
    private String roleId;
    
    /**
     * Pet instance ID
     */
    private Long petId;
    
    /**
     * Pet template/type ID
     */
    private Integer petTemplateId;
    
    /**
     * Old level before upgrade
     */
    private Integer oldLevel;
    
    /**
     * New level after upgrade
     */
    private Integer newLevel;
    
    /**
     * Levels gained
     */
    private Integer levelsGained;
    
    /**
     * Total experience gained
     */
    private Integer expGained;
    
    /**
     * Event timestamp (milliseconds)
     */
    private Long timestamp;
    
    /**
     * Event source
     */
    private String source = "pet-service";
}
