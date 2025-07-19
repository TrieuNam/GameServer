package com.example.ArenaService.dto.arena;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class MonsterDTO {
    @JsonProperty("seq")
    private String seq;
    @JsonProperty("monster_group")
    private String monsterGroup;
    @JsonProperty("monster_score")
    private String monsterScore;
    @JsonProperty("monstor_icon")
    private String monstorIcon;
    @JsonProperty("monster_fight")
    private String monsterFight;
    @JsonProperty("name_name")
    private String nameName;
    @JsonProperty("monster_level")
    private String monsterLevel;
    // getter/setter
}