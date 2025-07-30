package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShopShenmiOtherDTO {
    @JsonProperty("shuaxinjuan_id")
    private Integer shuaxinjuanId;
    @JsonProperty("shuaxinjuan_num")
    private Integer shuaxinjuanNum;
    @JsonProperty("shuaxin_item_num")
    private Integer shuaxinItemNum;
}