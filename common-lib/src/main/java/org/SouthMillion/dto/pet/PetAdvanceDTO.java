package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PetAdvanceDTO {
    @JsonProperty("pet_id")
    private int petId;
    @JsonProperty("pet_order")
    private int petOrder;
    @JsonProperty("need_myself")
    private int needMyself;
    @JsonProperty("need_myself_num")
    private int needMyselfNum;
    @JsonProperty("up_order_item_id")
    private int upOrderItemId;
    @JsonProperty("item_id_num")
    private int itemIdNum;
    @JsonProperty("up_att")
    private List<PetAttDTO> upAtt;

    @JsonProperty("unlock_skill_id")
    private Integer unlockSkillId;
}
