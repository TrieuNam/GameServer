package org.SouthMillion.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipEventDto {
    private String userId;
    private String type;      // "EQUIP_CHANGED", "EQUIP_DELETED", ...
    private Object payload;   // Có thể là EquipDto hoặc chỉ thông tin thay đổi
    private Long timestamp;
}