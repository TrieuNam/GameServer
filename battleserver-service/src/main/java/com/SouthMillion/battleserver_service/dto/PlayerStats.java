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
    private Integer vampiric;      // Percentage (0-100)
    private Integer vampiricImmunity;
    private Integer counter;
    private Integer counterImmunity;
    private Integer combo;
    private Integer comboImmunity;
    private Integer evasion;       // Percentage (0-100)
    private Integer evasionImmunity;
    private Integer criticalImmunity;
    private Integer stun;          // Percentage (0-100)
    private Integer stunImmunity;
    private Integer tyranny;       // Additional crit damage percentage
    private Integer benevolence;   // Crit damage reduction percentage
    private Integer muddy;
    private Integer interdiction;
    private Integer rejuvenation;
    private Integer level;
    private Integer fightPower;
}
