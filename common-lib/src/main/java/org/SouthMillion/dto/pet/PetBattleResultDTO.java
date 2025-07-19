package org.SouthMillion.dto.pet;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PetBattleResultDTO {
    private String winner; // "A", "B", "Draw"
    private int remainHpA;
    private int remainHpB;
    private int round;
}