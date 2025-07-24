package org.SouthMillion.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipDto {
    private Long id;
    private String userId;
    private int equipType;
    private int itemId;
    private int level;
    private int hp;
    private int attack;
    private int defend;
    private int speed;
    private Integer attrType1;   // Thêm nếu cần
    private Integer attrValue1;
    private Integer attrType2;
    private Integer attrValue2;
}
