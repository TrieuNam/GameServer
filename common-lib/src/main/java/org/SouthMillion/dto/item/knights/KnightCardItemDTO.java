package org.SouthMillion.dto.item.knights;

import lombok.Data;

import java.util.List;

@Data
public class KnightCardItemDTO {
    private List<RewardItemDTO> firstBuyRewardItem;
    private int buyMoney;
    private int time;
}