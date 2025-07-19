package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PetUpDTO {
    @JsonProperty("pet_type")
    private int petType;
    @JsonProperty("pet_level")
    private int petLevel;
    @JsonProperty("up_exp")
    private int upExp;
    @JsonProperty("up_att")
    private List<PetAttDTO> upAtt;
    @JsonProperty("abandon")
    private List<ItemNumDTO> abandon;
}
