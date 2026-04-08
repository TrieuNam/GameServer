package com.SouthMillion.starmap_service.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Starmap configuration loaded from starmap.json
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StarmapConfig {

    @JsonProperty("stars")
    private List<StarItem> stars;

    @JsonProperty("constellations")
    private List<ConstellationItem> constellations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StarItem {
        @JsonProperty("starId")
        private Integer starId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("constellationId")
        private Integer constellationId;

        @JsonProperty("baseAttributes")
        private Attributes baseAttributes;

        @JsonProperty("levelCost")
        private LevelCost levelCost;

        @JsonProperty("activationCost")
        private ActivationCost activationCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstellationItem {
        @JsonProperty("constellationId")
        private Integer constellationId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("totalStars")
        private Integer totalStars;

        @JsonProperty("starIds")
        private List<Integer> starIds;

        @JsonProperty("bonusAttributes")
        private Attributes bonusAttributes;

        @JsonProperty("unlockCost")
        private UnlockCost unlockCost;

        @JsonProperty("levelCost")
        private LevelCost levelCost;
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

        @JsonProperty("blessing")
        private Integer blessing; // Special celestial power bonus
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivationCost {
        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("materialItemId")
        private Integer materialItemId;

        @JsonProperty("materialCount")
        private Integer materialCount;

        @JsonProperty("energyCost")
        private Long energyCost;
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

        @JsonProperty("energyPerLevel")
        private Long energyPerLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnlockCost {
        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("materialItemId")
        private Integer materialItemId;

        @JsonProperty("materialCount")
        private Integer materialCount;

        @JsonProperty("requiredStarsActivated")
        private Integer requiredStarsActivated;
    }
}
