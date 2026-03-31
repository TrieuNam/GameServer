package com.SouthMillion.battleserver_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatRequest {
    private Long attackerId;
    private Long defenderId;
    private PlayerStats attacker;
    private PlayerStats defender;
    private String combatType; // PVP, PVE, BOSS, ARENA
    private Long timestamp;
}
