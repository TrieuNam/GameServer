package org.SouthMillion.dto.item.knights;

import lombok.Data;

import java.util.List;

@Data
public class KnightsRewardDTO {
    private int level;
    private List<JihuoAttDTO> jihuoAtt;
    private List<RewardItemDTO> reward;
}