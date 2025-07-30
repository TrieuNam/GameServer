package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HarnessBuyDTO {
    @JsonProperty("item_seq")
    private int itemSeq;
    @JsonProperty("harness_item")
    private int harnessItem;
    @JsonProperty("buy_item_id")
    private int buyItemId;
    @JsonProperty("buy_item_num")
    private int buyItemNum;
    private int rate1;
    private int rate2;
    @JsonProperty("rate2_change")
    private int rate2Change;
}