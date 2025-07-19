package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PetAttDTO {
    @JsonProperty("type")
    private int type;
    @JsonProperty("add")
    private int add;
}
