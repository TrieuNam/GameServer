package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClothShopItemDTO {
    @JsonProperty("shop_type")
    private Integer shopType;

    private Integer level;

    private Integer seq;

    @JsonProperty("group_id")
    private Integer groupId;

    @JsonProperty("item_id")
    private Integer itemId;

    @JsonProperty("item_num")
    private Integer itemNum;

    @JsonProperty("buy_item")
    private Integer buyItem;

    @JsonProperty("buy_item_num")
    private Integer buyItemNum;

    private Double discount;
}