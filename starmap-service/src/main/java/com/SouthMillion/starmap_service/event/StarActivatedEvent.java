package com.SouthMillion.starmap_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StarActivatedEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer starId;
    private Integer constellationId;
    private Integer starLevel;
    private Long timestamp = System.currentTimeMillis();
    private String source = "starmap-service";
    
    public StarActivatedEvent(Long userId, Integer starId, Integer constellationId, Integer starLevel) {
        this.userId = userId;
        this.starId = starId;
        this.constellationId = constellationId;
        this.starLevel = starLevel;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
