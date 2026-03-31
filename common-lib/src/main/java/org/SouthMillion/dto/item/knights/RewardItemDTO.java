package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single reward item returned from manual/handbook fetch-reward
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardItemDTO {

    @JsonProperty("item_id")
    private Integer itemId;

    @JsonProperty("num")
    private Integer num;

    @JsonProperty("item_type")
    private Integer itemType;
}
