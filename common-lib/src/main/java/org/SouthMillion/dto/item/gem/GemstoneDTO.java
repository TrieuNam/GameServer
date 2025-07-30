package org.SouthMillion.dto.item.gem;

import lombok.Data;

@Data
public class GemstoneDTO {
    private Integer id;
    private String name;
    private Integer itemType;
    private Integer up;
    private Integer tsGem;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private Integer param;
    private Integer gemLevel;
    private Integer getTheSource;
    private Integer oriId;
}