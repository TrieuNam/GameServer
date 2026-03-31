package com.SouthMillion.box_service.enity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="box_setting")
@Data
@NoArgsConstructor
@Getter
@Setter
public class BoxSetting {
    @Id
    private Long roleId;

    private int equipEqality;
    private int openFiveMark;
    private int equipCapMark;     // default 1
    private int equipSellMark;

    private int conditionFirst1;
    private int conditionFirst2;
    private int conditionSecond1;
    private int conditionSecond2;

    private int conditionFirstMark;
    private int conditionSecondMark;
    private int retainMark;
    private int challengeMark;
}