package org.SouthMillion.dto.user;

import lombok.Data;

@Data
public class BattleStateDTO {
    private Boolean inBattle;
    private Long battleId;

    public BattleStateDTO(Boolean inBattle, Long battleId) {
        this.inBattle = inBattle;
        this.battleId = battleId;
    }
}