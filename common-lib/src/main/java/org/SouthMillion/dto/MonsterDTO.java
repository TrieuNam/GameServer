package org.SouthMillion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MonsterDTO {
    @JsonProperty("monster_id")
    private String monsterId;

    @JsonProperty("monster_level")
    private String monsterLevel;

    private String name;

    @JsonProperty("res_id")
    private String resId;

    private String speed;
    private String hp;
    private String attack;
    private String defense;
    private String xixue;
    private String fanji;
    private String lianji;
    private String shanbi;
    private String baoji;
    private String jiyun;
    @JsonProperty("de_xixue")
    private String deXixue;
    @JsonProperty("de_fanji")
    private String deFanji;
    @JsonProperty("de_lianji")
    private String deLianji;
    @JsonProperty("de_shanbi")
    private String deShanbi;
    @JsonProperty("de_baoji")
    private String deBaoji;
    @JsonProperty("de_jiyun")
    private String deJiyun;
    @JsonProperty("skill_id")
    private String skillId;

    private String passiveSkillId;
}
