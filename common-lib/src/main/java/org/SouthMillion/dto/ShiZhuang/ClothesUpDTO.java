package org.SouthMillion.dto.ShiZhuang;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration DTO for clothes upgrade requirements deserialized from model_clothes.json "clothes_up" array
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothesUpDTO {

    @JsonProperty("clothes_id")
    private Integer clothesId;

    /** Target level this entry unlocks */
    @JsonProperty("level")
    private int level;

    /** Item id consumed as upgrade material */
    @JsonProperty("up_item_id")
    private Integer upItemId;

    /** Amount of material consumed */
    @JsonProperty("up_item_num")
    private Integer upItemNum;

    /** Gold cost for the upgrade */
    @JsonProperty("gold_cost")
    private Integer goldCost;
}
