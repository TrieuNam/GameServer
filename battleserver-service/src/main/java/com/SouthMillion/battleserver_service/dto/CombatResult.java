package com.SouthMillion.battleserver_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatResult {
    private Long attackerId;
    private Long defenderId;
    private Long winnerId;
    private Integer attackerFinalHp;
    private Integer defenderFinalHp;
    private Integer totalRounds;
    private Long duration;  // milliseconds
    private List<CombatRound> combatRounds;
}
