package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemNumDTO {
    @JsonProperty("item_id")
    private int itemId;
    @JsonProperty("num")
    private int num;
}
