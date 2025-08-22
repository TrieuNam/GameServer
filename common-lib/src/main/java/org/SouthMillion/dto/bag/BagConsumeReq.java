package org.SouthMillion.dto.bag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public record BagConsumeReq(
        String roleId,
        int bagType,
        List<Cost> cost
) {
    public record Cost(long itemId, long count){}
}