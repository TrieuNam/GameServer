package com.SouthMillion.starmap_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConstellationCompletedEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer constellationId;
    private Integer totalStars;
    private Integer level;
    private Long power;
    private Long timestamp = System.currentTimeMillis();
    private String source = "starmap-service";
    
    public ConstellationCompletedEvent(Long userId, Integer constellationId, 
                                      Integer totalStars, Integer level, Long power) {
        this.userId = userId;
        this.constellationId = constellationId;
        this.totalStars = totalStars;
        this.level = level;
        this.power = power;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
