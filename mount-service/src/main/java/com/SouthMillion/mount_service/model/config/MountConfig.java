package com.SouthMillion.mount_service.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Mount configuration loaded from config-service
 * Represents mount configuration JSON structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MountConfig {

    @JsonProperty("mount_list")
    private List<MountItem> mountList;

    @JsonProperty("level_costs")
    private List<LevelCost> levelCosts;

    @JsonProperty("grade_costs")
    private List<GradeCost> gradeCosts;

    @JsonProperty("star_costs")
    private List<StarCost> starCosts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MountItem {

        @JsonProperty("mount_id")
        private Integer mountId;

        @JsonProperty("mount_name")
        private String mountName;

        @JsonProperty("unlock_cost")
        private Long unlockCost;

        @JsonProperty("base_attributes")
        private Attributes baseAttributes;

        @JsonProperty("growth_rate")
        private GrowthRate growthRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attributes {

        @JsonProperty("hp")
        private Integer hp;

        @JsonProperty("attack")
        private Integer attack;

        @JsonProperty("defense")
        private Integer defense;

        @JsonProperty("speed")
        private Integer speed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrowthRate {

        @JsonProperty("hp_per_level")
        private Integer hpPerLevel;

        @JsonProperty("attack_per_level")
        private Integer attackPerLevel;

        @JsonProperty("defense_per_level")
        private Integer defensePerLevel;

        @JsonProperty("speed_per_level")
        private Integer speedPerLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LevelCost {

        @JsonProperty("level")
        private Integer level;

        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("material_id")
        private Integer materialId;

        @JsonProperty("material_count")
        private Integer materialCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GradeCost {

        @JsonProperty("grade")
        private Integer grade;

        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("material_id")
        private Integer materialId;

        @JsonProperty("material_count")
        private Integer materialCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StarCost {

        @JsonProperty("star_level")
        private Integer starLevel;

        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("material_id")
        private Integer materialId;

        @JsonProperty("material_count")
        private Integer materialCount;
    }
}
