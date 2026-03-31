package org.SouthMillion.dto.ShiZhuang;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration DTO for a single clothes/costume entry deserialized from model_clothes.json
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothesDTO {

    @JsonProperty("clothes_id")
    private Integer clothesId;

    @JsonProperty("name")
    private String name;

    /** Currency/item id used to buy */
    @JsonProperty("buy_money")
    private Integer buyMoney;

    /** Additional payment gold */
    @JsonProperty("add_pay_gold")
    private Integer addPayGold;

    /** Buy parameter 2 (reserved) */
    @JsonProperty("buy_param2")
    private Integer buyParam2;

    /** Quality tier (1-5) */
    @JsonProperty("quality")
    private Integer quality;
}
