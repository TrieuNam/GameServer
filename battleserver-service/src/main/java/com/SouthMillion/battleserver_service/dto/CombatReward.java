package com.SouthMillion.battleserver_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatReward {
    private Long winnerId;
    private Integer expGained;
    private Integer goldGained;
    private String itemDropped;
}
