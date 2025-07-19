package org.SouthMillion.dto;

import lombok.Data;

import java.util.List;

@Data
public class BattleResultDTO {
    private boolean win;
    private List<BattleStepDTO> battleSteps;
    private List<MonsterInstanceDTO> monstersAfterBattle;
}
