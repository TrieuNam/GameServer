package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PetSkillLevelDTO {
    @JsonProperty("id")
    private int id;
    @JsonProperty("index")
    private int index;
    @JsonProperty("name")
    private String name;
    @JsonProperty("skill_name")
    private String skillName;
    @JsonProperty("skill_txt")
    private String skillTxt;
    @JsonProperty("skill_type")
    private int skillType;
    @JsonProperty("skill_id_type")
    private int skillIdType;
    @JsonProperty("cloth_skill_level")
    private int clothSkillLevel;
}
