package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ThreeBoxItemDTO {
    @JsonProperty("item_id")
    private Integer itemId;
    private Integer num;
}