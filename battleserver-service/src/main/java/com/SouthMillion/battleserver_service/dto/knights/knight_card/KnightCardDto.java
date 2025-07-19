package com.SouthMillion.battleserver_service.dto.knights.knight_card;

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
public class KnightCardDto {
    @JsonProperty("first_buy_reward_item")
    private List<RewardItemDto> firstBuyRewardItem;

    @JsonProperty("buy_money")
    private String buyMoney;

    private String time;
}
