package org.SouthMillion.dto.bag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public record BagAddItemReq(
        String roleId,
        int bagType,
        List<Item> items
) {
    public record Item(long itemId, long count){}
}