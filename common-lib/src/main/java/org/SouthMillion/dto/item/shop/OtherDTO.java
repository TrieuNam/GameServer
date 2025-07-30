package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OtherDTO {
    @JsonProperty("price1")
    private Integer price1;

    @JsonProperty("price2")
    private Integer price2;

    @JsonProperty("reward_num")
    private Integer rewardNum;

    @JsonProperty("level_open")
    private Integer levelOpen;
}
