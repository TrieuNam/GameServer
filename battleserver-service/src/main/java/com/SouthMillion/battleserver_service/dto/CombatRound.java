package com.SouthMillion.battleserver_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatRound {
    private Integer round;
    private Long attackerId;
    private String skillId;
    private Integer damage;
    private Boolean critical;
    private Boolean dodged;
    private Boolean stunned;
    private Integer healingDone;
    private Integer targetRemainingHp;
}
