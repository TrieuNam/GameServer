package org.SouthMillion.dto.battle;

import lombok.Data;

import java.util.List;

@Data
public class PetInstanceDTO {
    private String instanceId;
    private String name;
    private int hp;
    private int attack;
    private int defense;
    private int speed;
    private List<String> skillIds;         // Danh sách skill chủ động
    private List<String> passiveSkillIds;  // Danh sách passive
}
