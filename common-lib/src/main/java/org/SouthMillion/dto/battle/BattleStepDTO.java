package org.SouthMillion.dto.battle;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BattleStepDTO {
    private int round;
    private List<ActionDTO> actions;
}