package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ThreeBoxDTO {
    @JsonProperty("box_oder")
    private Integer boxOrder;
    private List<ThreeBoxItemDTO> item;
}
