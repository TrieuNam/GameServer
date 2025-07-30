package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HarnessGemUseDTO {
    private int seq;
    @JsonProperty("use_item_id")
    private int useItemId;
    @JsonProperty("use_item_num")
    private int useItemNum;
    @JsonProperty("use_item_id2")
    private int useItemId2;
    @JsonProperty("use_item_num2")
    private int useItemNum2;
}