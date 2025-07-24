package org.SouthMillion.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEventDto {
    private String userId;
    private String type; // ITEM_CHANGED, EQUIP_CHANGED, ...
    private Object payload; // ItemData, EquipData, ...
    private Long timestamp;
}