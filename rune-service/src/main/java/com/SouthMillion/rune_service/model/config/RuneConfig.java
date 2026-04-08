package com.SouthMillion.rune_service.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Rune configuration model
 * Loaded from rune.json via config-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuneConfig {

    @JsonProperty("runeList")
    private List<RuneItem> runeList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuneItem {
        @JsonProperty("runeId")
        private Integer runeId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("baseAttributes")
        private Attributes baseAttributes;

        @JsonProperty("levelCost")
        private LevelCost levelCost;

        @JsonProperty("qualityCost")
        private QualityCost qualityCost;

        @JsonProperty("starCost")
        private StarCost starCost;

        @JsonProperty("refinementCost")
        private RefinementCost refinementCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attributes {
        @JsonProperty("hp")
        private Integer hp;

        @JsonProperty("attack")
        private Integer attack;

        @JsonProperty("defense")
        private Integer defense;

        @JsonProperty("speed")
        private Integer speed;

        @JsonProperty("critRate")
        private Integer critRate;

        @JsonProperty("critDamage")
        private Integer critDamage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelCost {
        @JsonProperty("goldPerLevel")
        private Long goldPerLevel;

        @JsonProperty("materialItemId")
        private Integer materialItemId;

        @JsonProperty("materialPerLevel")
        private Integer materialPerLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityCost {
        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("materialItemId")
        private Integer materialItemId;

        @JsonProperty("materialCount")
        private Integer materialCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StarCost {
        @JsonProperty("goldPerStar")
        private Long goldPerStar;

        @JsonProperty("materialItemId")
        private Integer materialItemId;

        @JsonProperty("materialPerStar")
        private Integer materialPerStar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefinementCost {
        @JsonProperty("goldPerLevel")
        private Long goldPerLevel;

        @JsonProperty("materialItemId")
        private Integer materialItemId;

        @JsonProperty("materialPerLevel")
        private Integer materialPerLevel;
    }
}
