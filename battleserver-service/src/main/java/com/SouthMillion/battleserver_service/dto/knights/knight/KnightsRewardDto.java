package com.SouthMillion.battleserver_service.dto.knights.knight;

import com.SouthMillion.battleserver_service.dto.knights.RewardItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnightsRewardDto {
    private String level;
    @JsonProperty("jihuo_att")
    private List<JihuoAttDto> jihuoAtt;
    private List<RewardItemDto> reward;
}