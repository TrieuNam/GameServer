package org.SouthMillion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleSkillDTO {
    @JsonProperty("skill_id")
    private String skillId;
    @JsonProperty("skill_level")
    private String skillLevel;
    @JsonProperty("target_side_type")
    private String targetSideType;
    @JsonProperty("target_num")
    private String targetNum;
    @JsonProperty("is_shanbi")
    private String isShanbi;
    @JsonProperty("is_baoji")
    private String isBaoji;
    @JsonProperty("is_lianji")
    private String isLianji;
    @JsonProperty("is_xixue")
    private String isXixue;
    @JsonProperty("is_jiyun")
    private String isJiyun;
    @JsonProperty("is_fanji")
    private String isFanji;
    @JsonProperty("effect_num")
    private String effectNum;
    @JsonProperty("effect_1")
    private String effect1;
    @JsonProperty("effect_2")
    private String effect2;
    @JsonProperty("effect_3")
    private String effect3;
    @JsonProperty("effect_4")
    private String effect4;
}
