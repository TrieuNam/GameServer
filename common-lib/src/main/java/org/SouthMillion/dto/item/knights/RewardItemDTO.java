package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RewardItemDTO {
    @JsonProperty("item_id")
    private int itemId;
    private int num;
}