package org.SouthMillion.dto.item.Box;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class BXZYConfigDTO {

    private List<Reward> reward;

    // Getter, Setter
    @Data
    @Setter
    @Getter
    public static class Reward {
        private String type;
        @JsonProperty("start_level")
        private String startLevel;
        @JsonProperty("end_level")
        private String endLevel;
        private String seq;
        @JsonProperty("reward_item")
        private List<RewardItem> rewardItem;
        @JsonProperty("price_type")
        private String priceType;
        @JsonProperty("limit_type")
        private String limitType;
        @JsonProperty("buy_times")
        private String buyTimes;
        @JsonProperty("buy_money")
        private String buyMoney;
        @JsonProperty("refresh_every_day")
        private String refreshEveryDay;
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
}
