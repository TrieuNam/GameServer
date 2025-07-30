package org.SouthMillion.dto.item.Knapsack;


import lombok.Data;

import java.util.List;

@Data
public class ItemRetrieveConfigDTO {
    private List<Retrieve> retrieve;
    private List<Level> level;
    private List<Other> other;

    @Data
    public static class Retrieve {
        private Integer seq;
        private Integer type;
        private Integer condition1;
        private Integer condition2;
        private Integer experienceRetrieve;
    }

    @Data
    public static class Level {
        private Integer retrieveLevel;
        private Integer upExp;
        private List<Attribute> upAtt;

        @Data
        public static class Attribute {
            private Integer type;
            private Integer add;
        }
    }

    @Data
    public static class Other {
        private Integer maxLevel;
    }
}