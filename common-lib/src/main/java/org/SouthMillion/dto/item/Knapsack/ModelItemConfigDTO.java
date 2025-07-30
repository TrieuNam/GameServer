package org.SouthMillion.dto.item.Knapsack;

import lombok.Data;

import java.util.List;

@Data
public class ModelItemConfigDTO {
    private Integer id;
    private String name;
    private Integer itemType;
    private Integer color;
    private List<Object> model;
    private List<Attribute> modelAfter;
    private Integer blockColorMin;
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