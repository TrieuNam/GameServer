package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PetDTO {
    @JsonProperty("pet_id")
    private int petId;
    @JsonProperty("pet_type")
    private int petType;
    @JsonProperty("pet_res")
    private int petRes;
    @JsonProperty("pet_name")
    private String petName;
    @JsonProperty("pet_icon")
    private int petIcon;
    @JsonProperty("pet_color")
    private int petColor;
    @JsonProperty("pet_att")
    private List<PetAttDTO> petAtt;
    @JsonProperty("skill_grid_max")
    private int skillGridMax;
    @JsonProperty("skill_grid_unlock")
    private String skillGridUnlock;
    // Business (giả lập):
    private List<Integer> skillIds = new ArrayList<>();
    private int petOrder = 1;
    private List<PetBuff> buffs = new ArrayList<>();
    private boolean stunned = false;
    private double nextDamageMultiplier = 1.0;
}