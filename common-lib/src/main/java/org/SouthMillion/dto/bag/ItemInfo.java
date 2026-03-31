package org.SouthMillion.dto.bag;

import lombok.*;

/**
 * Item information for grant requests
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemInfo {
    private Integer itemId;
    private Integer quantity;
    private Integer quality;
    private Boolean bound;
    
    public ItemInfo(Integer itemId, Integer quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
