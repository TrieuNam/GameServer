package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KnightZhengDTO {
    @JsonProperty("level_min")
    private int levelMin;
    @JsonProperty("level_max")
    private int levelMax;
    private int seq;
    @JsonProperty("guanggao_item")
    private RewardItemDTO guanggaoItem;
}