package org.SouthMillion.dto.pet;

import lombok.Data;

import java.util.List;

@Data
public class PetGameDTO {
    private int level;
    private int monster_id;
    private int monster_icon;
    private List<ItemNumDTO> win;
    private int rule_count;
    private int rule_id;
}
