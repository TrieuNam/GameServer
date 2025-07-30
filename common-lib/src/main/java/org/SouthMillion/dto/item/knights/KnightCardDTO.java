package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KnightCardDTO {
    @JsonProperty("first_buy_reward_item")
    private List<RewardItemDTO> firstBuyRewardItem;
    @JsonProperty("buy_money")
    private int buyMoney;
    private int time;
}