package org.SouthMillion.dto.pet;

import lombok.Data;

@Data
public class PetEvoDTO {
    private int pet_type;
    private int evo_item_id;
    private int item_id_num;
    private int pet_type_after;
    private int pet_res_before;
    private int pet_res_after;
}