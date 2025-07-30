package org.SouthMillion.dto.item.Knapsack;

import lombok.Data;

import java.util.List;

@Data
public class TitleItemConfigDTO {
    private Integer id;
    private String name;
    private Integer itemType;
    private Integer quality;
    private Integer sellprice;
    private Integer pileLimit;
    private Integer isdroprecord;
    private Integer invalidTime;
    private List<Attribute> titleAtt;

    @Data
    public static class Attribute {
        private Integer type;
        private Integer add;
    }
}
