package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single item entry in the clothes shop configuration (cloth_shop.json)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothShopItemDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("clothes_id")
    private Integer clothesId;

    @JsonProperty("name")
    private String name;

    /** Currency / item id used to pay */
    @JsonProperty("buy_item")
    private Integer buyItem;

    /** Cost amount */
    @JsonProperty("buy_item_num")
    private Integer buyItemNum;

    /** Discount multiplier (1.0 = no discount) */
    @JsonProperty("discount")
    private Double discount;

    /** Minimum player level required */
    @JsonProperty("level_min")
    private Integer levelMin;

    /** Maximum player level allowed (0 or null = unlimited) */
    @JsonProperty("level_max")
    private Integer levelMax;
}
