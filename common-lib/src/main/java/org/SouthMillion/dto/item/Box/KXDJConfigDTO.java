package org.SouthMillion.dto.item.Box;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class KXDJConfigDTO {

    private List<Reward> reward;

    private List<Other> other;

    // Getter, Setter
    @Data
    @Setter
    @Getter
    public static class Reward {
        private String type;
        @JsonProperty("type_box_num")
        private String typeBoxNum;
        @JsonProperty("type_num")
        private String typeNum;
        @JsonProperty("reward_item")
        private RewardItem rewardItem;
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
    public static class Other {
        private String time;
        @JsonProperty("is_open")
        private String isOpen;
        // Getter, Setter
    }
}