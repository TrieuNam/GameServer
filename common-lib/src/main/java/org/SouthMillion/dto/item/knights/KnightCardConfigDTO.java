package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KnightCardConfigDTO {
    @JsonProperty("knight_card")
    private List<KnightCardDTO> knightCard;
    @JsonProperty("knight_zheng")
    private List<KnightZhengDTO> knightZheng;
}
