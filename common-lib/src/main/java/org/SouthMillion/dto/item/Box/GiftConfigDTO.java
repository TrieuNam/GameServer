package org.SouthMillion.dto.item.Box;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class GiftConfigDTO {

    @JsonProperty("DefGift")
    private List<Gift> defGift;

    @JsonProperty("RandGift")
    private List<Gift> randGift;

    // Getter, Setter
    @Data
    @Setter
    @Getter
    public static class Gift {
        private String id;
        private String name;
        @JsonProperty("item_type")
        private String itemType;
        @JsonProperty("gift_type")
        private String giftType;
        private String color;
        @JsonProperty("pile_limit")
        private String pileLimit;
        private String isdroprecord;
        @JsonProperty("invalid_time")
        private String invalidTime;
        @JsonProperty("item_num")
        private String itemNum;
        @JsonProperty("rand_num")
        private String randNum;
        private List<GiftItem> gift;
        private String sellprice;
        // Getter, Setter
    }
    @Data
    @Setter
    @Getter
    public static class GiftItem {
        @JsonProperty("item_id")
        private String itemId;
        private String num;
        private String rate;
        // Getter, Setter
    }
}
