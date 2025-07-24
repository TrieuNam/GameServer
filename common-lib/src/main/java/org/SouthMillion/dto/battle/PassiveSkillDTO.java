package org.SouthMillion.dto.battle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PassiveSkillDTO {
    @JsonProperty("skill_id")
    private String skillId;
    @JsonProperty("skill_level")
    private String skillLevel;
    @JsonProperty("skill_desc")
    private String skillDesc;
    @JsonProperty("att_type")
    private String attType;
    @JsonProperty("skill_att_type")
    private String skillAttType;
    private String att_num2;
    private String att_num3;
    private String att_num4;
    private String att_num5;
    private String att_num6;
    @JsonProperty("skill_times")
    private String skillTimes;
}
