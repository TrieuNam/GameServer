package org.SouthMillion.dto.item.Box;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class OtherItemConfigDTO {

    @JsonProperty("Other")
    private List<OtherItem> other;

    // Getter, Setter
    @Data
    @Setter
    @Getter
    public static class OtherItem {
        private String id;
        private String name;
        @JsonProperty("item_type")
        private String itemType;
        @JsonProperty("is_virtual")
        private String isVirtual;
        private String sellprice;
        @JsonProperty("pile_limit")
        private String pileLimit;
        private String isdroprecord;
        @JsonProperty("invalid_time")
        private String invalidTime;
        private String param;
        @JsonProperty("get_the_source")
        private String getTheSource;
        // Getter, Setter
    }
}