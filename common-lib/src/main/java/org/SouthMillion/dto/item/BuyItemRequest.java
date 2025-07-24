package org.SouthMillion.dto.item;

import lombok.Data;

@Data
public class BuyItemRequest {
    private String userId;
    private int itemId;
    private int amount;
}