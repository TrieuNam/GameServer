package com.SouthMillion.battleserver_service.dto.knights.knight_card;

import com.SouthMillion.battleserver_service.dto.knights.RewardItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnightZhengDto {
    @JsonProperty("level_min")
    private String levelMin;
    @JsonProperty("level_max")
    private String levelMax;
    private String seq;

    @JsonProperty("guanggao_item")
    private RewardItemDto guanggaoItem;
}