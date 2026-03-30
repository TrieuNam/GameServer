package com.SouthMillion.angel_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when an angel evolves/advances
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AngelEvolvedEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer angelId;
    private Integer angelIndex;
    private Integer oldEvolutionStage;
    private Integer newEvolutionStage;
    private Integer currentLevel;
    private Long timestamp = System.currentTimeMillis();
    private String source = "angel-service";
    
    public AngelEvolvedEvent(Long userId, Integer angelId, Integer angelIndex, 
                            Integer oldEvolutionStage, Integer newEvolutionStage, 
                            Integer currentLevel) {
        this.userId = userId;
        this.angelId = angelId;
        this.angelIndex = angelIndex;
        this.oldEvolutionStage = oldEvolutionStage;
        this.newEvolutionStage = newEvolutionStage;
        this.currentLevel = currentLevel;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
