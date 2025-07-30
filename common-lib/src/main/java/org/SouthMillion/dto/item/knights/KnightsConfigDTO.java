package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KnightsConfigDTO {
    @JsonProperty("knights_book")
    private List<KnightsBookDTO> knightsBook;
    @JsonProperty("knights_reward")
    private List<KnightsRewardDTO> knightsReward;
}
