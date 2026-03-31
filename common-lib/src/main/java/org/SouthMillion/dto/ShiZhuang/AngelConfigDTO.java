package org.SouthMillion.dto.ShiZhuang;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root config DTO deserialized from angel.json
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AngelConfigDTO {

    @JsonProperty("angel_cfg")
    private List<AngelLevelCfg> angelCfg;

    @JsonProperty("angel_up")
    private List<AngelUpCfg> angelUp;

    @JsonProperty("equipment_up")
    private List<AngelEquipUpCfg> equipmentUp;

    @JsonProperty("angel_res")
    private List<AngelSkinCfg> angelRes;

    @JsonProperty("angel_res_up")
    private List<AngelSkinUpCfg> angelResUp;

    // ---- Inner classes ----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AngelLevelCfg {
        @JsonProperty("level")
        private int level;

        @JsonProperty("up_item_id")
        private int upItemId;

        @JsonProperty("up_item_num")
        private int upItemNum;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AngelUpCfg {
        @JsonProperty("angle_stage")
        private int angleStage;

        @JsonProperty("stage_item_id")
        private int stageItemId;

        @JsonProperty("stage_item_num")
        private int stageItemNum;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AngelEquipUpCfg {
        @JsonProperty("position")
        private int position;

        @JsonProperty("equipment_id")
        private int equipmentId;

        @JsonProperty("up_item_id0")
        private int upItemId0;

        @JsonProperty("up_item_num0")
        private int upItemNum0;

        @JsonProperty("up_item_id1")
        private int upItemId1;

        @JsonProperty("up_item_num1")
        private int upItemNum1;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AngelSkinCfg {
        @JsonProperty("angle_skin_seq")
        private int angleSkinSeq;

        @JsonProperty("jihuo_item_id")
        private int jihuoItemId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AngelSkinUpCfg {
        @JsonProperty("angle_skin_seq")
        private int angleSkinSeq;

        @JsonProperty("skin_level")
        private int skinLevel;

        @JsonProperty("up_item_id")
        private int upItemId;

        @JsonProperty("up_item_num")
        private int upItemNum;
    }
}
