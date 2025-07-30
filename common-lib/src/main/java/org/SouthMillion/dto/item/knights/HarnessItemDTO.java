package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class HarnessItemDTO {
    private int id;
    private String name;
    @JsonProperty("harness_type")
    private int harnessType;
    private List<JihuoAttDTO> att;
    @JsonProperty("harness_own_att_num")
    private int harnessOwnAttNum;
    @JsonProperty("harness_att_num_max")
    private int harnessAttNumMax;
    @JsonProperty("sell_item_id")
    private int sellItemId;
    @JsonProperty("sell_item_num")
    private int sellItemNum;
    @JsonProperty("item_type")
    private int itemType;
    private int sellprice;
    @JsonProperty("pile_limit")
    private int pileLimit;
    private int isdroprecord;
    @JsonProperty("invalid_time")
    private int invalidTime;
    @JsonProperty("get_the_source")
    private int getTheSource;
}