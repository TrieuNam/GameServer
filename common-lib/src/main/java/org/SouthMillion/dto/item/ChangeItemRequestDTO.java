package org.SouthMillion.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeItemRequestDTO {
    private Long itemId;          // For single item operations
    private Integer count;        // For single item operations
    private List<ChangeItemPair> items; // For bulk operations
}
