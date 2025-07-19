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
public class SingleSkillEffectDTO {
    private String seq;
    @JsonProperty("effect_type")
    private String effectType;
    private String param1;
    private String param2;
    private String param3;
    private String param4;
}