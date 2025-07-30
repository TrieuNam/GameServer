package org.SouthMillion.dto.item.Knapsack;

import lombok.Data;

import java.util.List;

@Data
public class HarnessItemConfigDTO {
    private Integer id;
    private String name;
    private Integer harnessType;
    private List<Attribute> att;
    private Integer harnessOwnAttNum;
    private Integer harnessAttNumMax;
    private Integer sellItemId;
    private Integer sellItemNum;
    private Integer itemType;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private Integer getTheSource;

    @Data
    public static class Attribute {
        private Integer type;
        private Integer add;
    }
}