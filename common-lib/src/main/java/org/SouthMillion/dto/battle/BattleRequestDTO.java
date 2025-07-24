package org.SouthMillion.dto.battle;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BattleRequestDTO {
    private String playerId;
    private int playerAttack, playerDefense, playerSpeed;
    private List<String> playerSkillIds;
    private List<String> playerPassiveSkillIds;
    private List<PetInstanceDTO> petInstances;        // <--- Thêm dòng này!
    private List<MonsterInstanceDTO> monsters;
}