package com.SouthMillion.rune_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuneCreatedEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer runeId;
    private Integer runeIndex;
    private Integer quality;
    private Long timestamp = System.currentTimeMillis();
    private String source = "rune-service";
    
    public RuneCreatedEvent(Long userId, Integer runeId, Integer runeIndex, Integer quality) {
        this.userId = userId;
        this.runeId = runeId;
        this.runeIndex = runeIndex;
        this.quality = quality;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
