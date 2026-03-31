package com.SouthMillion.pet_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pet Evolution Event
 * Published when a pet evolves (star rating increases)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetEvolvedEvent {
    
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
     * Old star rating before evolution
     */
    private Integer oldStar;
    
    /**
     * New star rating after evolution
     */
    private Integer newStar;
    
    /**
     * Pet level at evolution
     */
    private Integer petLevel;
    
    /**
     * Event timestamp (milliseconds)
     */
    private Long timestamp;
    
    /**
     * Event source
     */
    private String source = "pet-service";
}
