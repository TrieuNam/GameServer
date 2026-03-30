package com.SouthMillion.arenaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaPlayerDTO {
    private String playerId;
    private Integer rating;
    private Integer wins;
    private Integer losses;
    private Integer currentRank;
    private Integer consecutiveWins;
    private Integer totalBattles;
    private Integer winRate;
    private Integer season;
}
