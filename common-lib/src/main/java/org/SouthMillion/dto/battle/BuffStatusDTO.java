package org.SouthMillion.dto.battle;

import lombok.Data;

@Data
public class BuffStatusDTO {
    private String buffId;
    private String name;
    private String effectType; // stun, shield, heal, reflect, poison, immune...
    private int value;         // số máu shield, % reflect, % heal, % poison
    private int duration;      // số lượt còn lại
    private String from;       // ai cast
}
