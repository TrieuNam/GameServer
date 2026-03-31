package com.SouthMillion.pet_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pet Activated Event
 * Published when a new pet is unlocked/activated for the first time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetActivatedEvent {
    
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
     * Initial level (usually 1)
     */
    private Integer initialLevel;
    
    /**
     * Initial star rating (usually 1)
     */
    private Integer initialStar;
    
    /**
     * Event timestamp (milliseconds)
     */
    private Long timestamp;
    
    /**
     * Event source
     */
    private String source = "pet-service";
}
