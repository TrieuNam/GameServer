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
        Boolean isVirtual,  // true = virtual/currency item (go to wallet, not bag)

        // Optional equipment metadata from `gameworld/item/equipment.json`
        Integer equipType,
        Integer level,
        Integer hp,
        Integer attack,
        Integer defend,
        Integer speed,
        Integer attrType1,
        Integer attrValue1,
        Integer attrType2,
        Integer attrValue2
) {}