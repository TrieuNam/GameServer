package org.SouthMillion.dto.item.Box;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class BXJJConfigDTO {

    @JsonProperty("gift_configure")
    private List<GiftConfigure> giftConfigure;

    @JsonProperty("phase_configure")
    private List<PhaseConfigure> phaseConfigure;

    private List<Other> other;

    @JsonProperty("item_reward")
    private List<List<Object>> itemReward;

    // Getter, Setter
    @Data
    @Setter
    @Getter
    public static class GiftConfigure {
        private String seq;
        private String phase;
        private String level;
        @JsonProperty("ordinary_item")
        private OrdinaryItem ordinaryItem;
        @JsonProperty("senior_item")
        private List<OrdinaryItem> seniorItem;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class OrdinaryItem {
        @JsonProperty("item_id")
        private String itemId;
        private String num;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class PhaseConfigure {
        private String phase;
        @JsonProperty("buy_money")
        private String buyMoney;
        @JsonProperty("show_level")
        private String showLevel;
        // Getter, Setter
    }

    @Data
    @Setter
    @Getter
    public static class Other {
        private String phase;
        @JsonProperty("multiple_Ordinary_reward")
        private String multipleOrdinaryReward;
        @JsonProperty("accumulate_recharge_show")
        private String accumulateRechargeShow;
        @JsonProperty("is_open")
        private String isOpen;
        // Getter, Setter
    }
}