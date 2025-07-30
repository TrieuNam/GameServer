package org.SouthMillion.dto.item.Box;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class UnpackConfigDTO {

    @JsonProperty("random_level")
    private List<RandomLevel> randomLevel;

    @JsonProperty("random_color")
    private List<RandomColor> randomColor;

    @JsonProperty("other")
    private List<UnpackOther> other;

    @JsonProperty("color_att")
    private List<ColorAtt> colorAtt;

    @JsonProperty("att_describe")
    private List<List<Object>> attDescribe;

    @JsonProperty("auto_unpack")
    private List<List<Object>> autoUnpack;

    @JsonProperty("shizhuang_rate")
    private List<ShizhuangRate> shizhuangRate;

    @JsonProperty("fixed_reward")
    private List<FixedReward> fixedReward;

    @JsonProperty("getway")
    private List<List<Object>> getWay;

    // Getter, Setter
    @Data
    @Setter
    @Getter
    public static class RandomLevel {
        private String level;
        @JsonProperty("random_level")
        private String randomLevel;
        private String rate;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class RandomColor {
        @JsonProperty("box_level")
        private String boxLevel;
        @JsonProperty("equipment_color_1")
        private String equipmentColor1;
        @JsonProperty("equipment_color_2")
        private String equipmentColor2;
        @JsonProperty("up_buy_num")
        private String upBuyNum;
        private String price;
        @JsonProperty("up_time_minute")
        private String upTimeMinute;
        private List<RewardItem> reward;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class RewardItem {
        @JsonProperty("item_id")
        private String itemId;
        private String num;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class UnpackOther {
        @JsonProperty("additional_attribute_num")
        private String additionalAttributeNum;
        @JsonProperty("box_num_max")
        private String boxNumMax;
        @JsonProperty("currency_type")
        private String currencyType;
        @JsonProperty("accelerate_id")
        private String accelerateId;
        @JsonProperty("unpack_item_id")
        private String unpackItemId;
        private String challenge;
        @JsonProperty("get_rate")
        private String getRate;
        @JsonProperty("get_num")
        private String getNum;
        @JsonProperty("down_rate")
        private String downRate;
        @JsonProperty("max_challenge")
        private String maxChallenge;
        @JsonProperty("get_shizhuang")
        private String getShizhuang;
        @JsonProperty("max_shizhuang")
        private String maxShizhuang;
        @JsonProperty("shizhuang_1")
        private String shizhuang1;
        @JsonProperty("shizhuang_2")
        private String shizhuang2;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class ColorAtt {
        @JsonProperty("att_group")
        private String attGroup;
        @JsonProperty("att_type")
        private String attType;
        @JsonProperty("att_num_min")
        private String attNumMin;
        @JsonProperty("att_num_max")
        private String attNumMax;
        private String rate;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class ShizhuangRate {
        private String seq;
        @JsonProperty("item_id")
        private String itemId;
        private String rate;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class FixedReward {
        @JsonProperty("box_oder")
        private String boxOder;
        @JsonProperty("item_id")
        private String itemId;
        // Getter, Setter
    }
}