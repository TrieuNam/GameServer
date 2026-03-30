package org.SouthMillion.dto.item;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemMetaDTO(
        int itemId,
        String itemType,    // equip/consumable/currency/unknown
        Integer quality,
        Long exp,
        Long sellPrice,
        Integer pileLimit,
        Long invalidTime,
        Boolean isSpecial,
        Boolean isVirtual   // true = virtual/currency item (go to wallet, not bag)
) {}