package org.SouthMillion.dto.battle;

import lombok.Data;

import java.util.List;

@Data
public class MonsterInstanceDTO {
    private String instanceId;
    private MonsterDTO monster;
    private int hp, attack, defense, speed;
    private boolean alive;
    private List<String> skillIds;
    private List<String> passiveSkillIds;
    private List<BuffStatusDTO> activeBuffs;
    private String state;
}