package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HarnessAddDTO {
    @JsonProperty("harness_id")
    private int harnessId;
    private int seq;
    @JsonProperty("add_type")
    private int addType;
    @JsonProperty("range_a")
    private String rangeA;
    @JsonProperty("range_1")
    private String range1;
    @JsonProperty("rate_1")
    private int rate1;
    // ... các field còn lại tuỳ theo cấu hình
}