package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PetSkillDTO {
    @JsonProperty("skill_seq")
    private int skillSeq;
    @JsonProperty("skill_color")
    private int skillColor;
    @JsonProperty("skill_page")
    private int skillPage;
    @JsonProperty("skill_id")
    private int skillId;
    @JsonProperty("skill_item_id")
    private int skillItemId;
    @JsonProperty("skill_name")
    private String skillName;
    @JsonProperty("skill_decs")
    private String skillDecs;

    private List<SkillEffect> randomEffects; // các hiệu ứng phụ
}
