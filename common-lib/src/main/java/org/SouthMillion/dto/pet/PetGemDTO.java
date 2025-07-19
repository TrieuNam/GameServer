package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PetGemDTO {
    @JsonProperty("gem_id")
    private int gemId;
    @JsonProperty("gem_type")
    private int gemType;
    @JsonProperty("level")
    private int level;
    @JsonProperty("up_att")
    private List<PetAttDTO> upAtt;
    @JsonProperty("up_need_same_class")
    private int upNeedSameClass;
    @JsonProperty("gem_num")
    private int gemNum;
    @JsonProperty("gem_level1")
    private int gemLevel1;
}
