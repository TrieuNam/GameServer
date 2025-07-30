package org.SouthMillion.dto.item.Box;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BoxSetDTO {
    private Integer equipEqality;
    private Integer conditionFirstMark;
    private Integer conditionFirst1;
    private Integer conditionFirst2;
    private Integer conditionSecondMark;
    private Integer conditionSecond1;
    private Integer conditionSecond2;
    private Integer retainMark;
    private Integer challengeMark;
    private Integer equipCapMark;
    private Integer equipSellMark;
    private Integer openFiveMark;
}