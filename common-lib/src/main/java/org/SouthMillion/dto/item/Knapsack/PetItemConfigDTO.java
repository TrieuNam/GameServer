package org.SouthMillion.dto.item.Knapsack;

import lombok.Data;

@Data
public class PetItemConfigDTO {
    private Integer id;
    private String name;
    private Integer itemType;
    private Integer isVirtual;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private Integer param;
    private Integer getTheSource;
}