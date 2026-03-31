package com.SouthMillion.rune_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuneEquippedEvent {
    private String eventId = UUID.randomUUID().toString();
    private Long userId;
    private Integer runeId;
    private Integer runeIndex;
    private Integer equipSlot;
    private Long combatPower;
    private Long timestamp = System.currentTimeMillis();
    private String source = "rune-service";
    
    public RuneEquippedEvent(Long userId, Integer runeId, Integer runeIndex, 
                           Integer equipSlot, Long combatPower) {
        this.userId = userId;
        this.runeId = runeId;
        this.runeIndex = runeIndex;
        this.equipSlot = equipSlot;
        this.combatPower = combatPower;
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }
}
