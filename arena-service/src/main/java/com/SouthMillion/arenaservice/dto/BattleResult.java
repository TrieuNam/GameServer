package com.SouthMillion.arenaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleResult {
    private String winnerId;
    private String loserId;
    private Integer ratingChange;
    private Integer winnerRatingBefore;
    private Integer winnerRatingAfter;
    private Integer loserRatingBefore;
    private Integer loserRatingAfter;
    private Integer battleDuration;
    private Boolean isConsecutiveWin;
    private Integer consecutiveWinBonus;
}
