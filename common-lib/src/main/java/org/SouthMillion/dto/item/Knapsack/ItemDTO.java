package org.SouthMillion.dto.item.Knapsack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single item in a player's knapsack/bag
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {

    @JsonProperty("item_id")
    private Integer itemId;

    @JsonProperty("num")
    private Integer num;

    @JsonProperty("item_type")
    private Integer itemType;

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("quality")
    private Integer quality;
}
