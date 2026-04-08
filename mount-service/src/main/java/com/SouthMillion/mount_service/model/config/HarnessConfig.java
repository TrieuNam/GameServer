package com.SouthMillion.mount_service.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Harness configuration loaded from config-service
 * Represents harness.json structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessConfig {

    @JsonProperty("harness_list")
    private List<HarnessItem> harnessList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HarnessItem {

        @JsonProperty("harness_id")
        private Integer harnessId;

        @JsonProperty("harness_name")
        private String harnessName;

        @JsonProperty("quality")
        private Integer quality; // 1=white, 2=green, 3=blue, 4=purple, 5=orange

        @JsonProperty("base_attributes")
        private Attributes baseAttributes;

        @JsonProperty("buy_cost")
        private BuyCost buyCost;

        @JsonProperty("upgrade_cost")
        private UpgradeCost upgradeCost;
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
    public static class BuyCost {

        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("diamond")
        private Long diamond;

        @JsonProperty("item_id")
        private Integer itemId;

        @JsonProperty("item_count")
        private Integer itemCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpgradeCost {

        @JsonProperty("gold")
        private Long gold;

        @JsonProperty("material_id")
        private Integer materialId;

        @JsonProperty("material_count")
        private Integer materialCount;
    }
}
