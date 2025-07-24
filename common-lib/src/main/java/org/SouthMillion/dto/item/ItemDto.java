package org.SouthMillion.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDto {
    private Long id;        // id trong DB, có thể null nếu chưa lưu
    private String userId;
    private int itemId;
    private int amount;
}