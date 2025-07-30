package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ShopShenmiFile {
    private List<ShopShenmiDTO> shop;
    private List<ShopShenmiOtherDTO> other;

    @JsonProperty("level_to_index")
    private List<LevelToIndexDTO> levelToIndex;
}
