package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class HarnessItemConfigDTO {
    @JsonProperty("harness_item")
    private List<HarnessItemDTO> harnessItem;
}
