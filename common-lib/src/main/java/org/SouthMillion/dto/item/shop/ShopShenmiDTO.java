package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShopShenmiDTO {
    private Integer index;
    @JsonProperty("level_min")
    private Integer levelMin;
    @JsonProperty("level_max")
    private Integer levelMax;
    @JsonProperty("item_id")
    private Integer itemId;
    @JsonProperty("item_num")
    private Integer itemNum;
    @JsonProperty("exchange_item_id")
    private Integer exchangeItemId;
    @JsonProperty("exchange_item_num")
    private Integer exchangeItemNum;
    @JsonProperty("permanent_buy")
    private Integer permanentBuy;
    private Integer rate;
}
