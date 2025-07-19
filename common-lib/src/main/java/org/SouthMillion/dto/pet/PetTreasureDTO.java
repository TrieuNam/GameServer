package org.SouthMillion.dto.pet;

import lombok.Data;

import java.util.List;

@Data
public class PetTreasureDTO {
    private int level_min;
    private int level_max;
    private int type;
    private int rate;
    private List<ItemNumDTO> win;
}
