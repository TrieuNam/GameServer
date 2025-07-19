package org.SouthMillion.dto.pet;

import lombok.Data;

import java.util.List;

@Data
public class PetTsGemDTO {
    private int ts_gem_level;
    private int up_need_same_class;
    private int gem_num;
    private List<PetAttDTO> up_att;
    private int up_att_num;
    private int gem_level1;
    private int to_item_id;
}