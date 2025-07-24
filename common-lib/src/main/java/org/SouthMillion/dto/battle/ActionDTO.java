package org.SouthMillion.dto.battle;

import lombok.Data;

import java.util.List;

@Data
public class ActionDTO {
    private String actorId;
    private String skillId;
    private String targetId;
    private int damage;
    private String effect; // stun, poison, heal...
    private List<BuffStatusDTO> buffsApplied;
    private boolean critical;
    private boolean miss;
    private boolean dead;
}
