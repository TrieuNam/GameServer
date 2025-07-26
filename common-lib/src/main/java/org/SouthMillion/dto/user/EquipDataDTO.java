package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Setter
@Getter
public class EquipDataDTO implements Serializable {
    private Integer equipType;       // 装备类型（部位）
    private Integer itemId;          // 物品ID
    private Integer hp;              // 血量
    private Integer attack;          // 攻击
    private Integer defend;          // 防御
    private Integer speed;           // 速度
    private Integer attrType1;       // 属性类型1
    private Integer attrValue1;      // 属性类型1值
    private Integer attrType2;       // 属性类型2
    private Integer attrValue2;      // 属性类型2值
}