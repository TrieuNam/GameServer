package org.SouthMillion.dto.item.Knapsack;

import lombok.Data;

@Data
public class BlockItemConfigDTO {
    private Integer id;
    private String name;
    private Integer itemType;
    private Integer color;
    private String blockRange;
    private Integer shape;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private Integer getTheSource;
}
