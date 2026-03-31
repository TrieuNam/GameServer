package org.SouthMillion.dto.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for player manual/handbook information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualInfoDTO {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("current_level")
    private int currentLevel;

    @JsonProperty("current_exp")
    private long currentExp;

    @JsonProperty("total_exp")
    private long totalExp;

    @JsonProperty("unlocked_items")
    private List<Integer> unlockedItems;
}
