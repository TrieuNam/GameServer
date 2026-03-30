package com.SouthMillion.battleserver_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStats {
    private Long playerId;
    private Integer hp;
    private Integer maxHp;
    private Integer attack;
    private Integer defense;
    private Integer speed;
    private Integer critRate;      // Percentage (0-100)
    private Integer critDamage;    // Percentage (100-300)
    private Integer level;
    private Integer fightPower;
}
