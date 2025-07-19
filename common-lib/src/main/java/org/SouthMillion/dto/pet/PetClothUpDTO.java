package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PetClothUpDTO {
    @JsonProperty("id")
    private int id;
    @JsonProperty("petcloth_order")
    private int petclothOrder;
    @JsonProperty("petcloth_count")
    private int petclothCount;
}
