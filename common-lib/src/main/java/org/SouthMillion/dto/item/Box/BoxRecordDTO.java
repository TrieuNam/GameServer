package org.SouthMillion.dto.item.Box;

import lombok.Data;

@Data
public class BoxRecordDTO {
    private Long userId;
    private String boxType;
    private Integer boxId;
    private Integer rewardItemId;
    private Integer rewardNum;
    private Integer sellPrice;
    private Integer exp;
    private Integer timestamp;
    // ... field khác nếu cần

    private Integer equipType;
    private Integer hp;
    private Integer attack;
    private Integer defend;
    private Integer speed;
    private Integer attrType1;
    private Integer attrValue1;
    private Integer attrType2;
    private Integer attrValue2;
}
