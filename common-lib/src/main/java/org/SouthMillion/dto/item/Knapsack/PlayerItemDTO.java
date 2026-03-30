package org.SouthMillion.dto.item.Knapsack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a player-owned item entry from the knapsack
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerItemDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("role_id")
    private Long roleId;

    @JsonProperty("item_id")
    private Integer itemId;

    @JsonProperty("num")
    private Integer num;

    @JsonProperty("item_type")
    private Integer itemType;
}
