package org.SouthMillion.dto.item.gem;

import lombok.Data;

@Data
public class GemstoneDrawingDTO {
    private Integer id;
    private String name;
    private Integer itemType;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private Integer param;
    private Integer getTheSource;
}