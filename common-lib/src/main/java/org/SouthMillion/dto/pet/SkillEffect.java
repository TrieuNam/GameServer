package org.SouthMillion.dto.pet;

import lombok.Data;

@Data
public class SkillEffect {
    private String effectType; // STUN, DOUBLE_DAMAGE...
    private int rate; // xác suất (phần trăm)
    private int value;
}
