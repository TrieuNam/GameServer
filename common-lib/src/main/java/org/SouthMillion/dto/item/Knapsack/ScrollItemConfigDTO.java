package org.SouthMillion.dto.item.Knapsack;

import lombok.Data;

@Data
public class ScrollItemConfigDTO {
    private Integer id;
    private String name;
    private Integer type;
    private Integer sellItemId;
    private Integer sellItemNum;
    private Integer itemType;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private String getTheSource;
}
