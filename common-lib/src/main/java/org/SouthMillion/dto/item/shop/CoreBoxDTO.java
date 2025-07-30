package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CoreBoxDTO {
    @JsonProperty("box_type")
    private Integer boxType;

    @JsonProperty("box_item")
    private Integer boxItem;

    @JsonProperty("box_item_min")
    private Integer boxItemMin;

    @JsonProperty("box_item_max")
    private Integer boxItemMax;

    @JsonProperty("box_rate")
    private Integer boxRate;
}
