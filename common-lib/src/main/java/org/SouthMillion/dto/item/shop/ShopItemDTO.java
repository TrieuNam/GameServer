package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShopItemDTO {
    private Integer index;
    private Integer level;
    private Integer seq;
    private Integer page;
    @JsonProperty("page_1")
    private Integer page1;
    @JsonProperty("item_id")
    private Integer itemId;
    @JsonProperty("item_num")
    private Integer itemNum;
    @JsonProperty("exchange_item_id")
    private Integer exchangeItemId;
    @JsonProperty("exchange_item_num")
    private Integer exchangeItemNum;
    @JsonProperty("quota_type")
    private Integer quotaType;
    private Integer param;
    @JsonProperty("show_level")
    private Integer showLevel;
}