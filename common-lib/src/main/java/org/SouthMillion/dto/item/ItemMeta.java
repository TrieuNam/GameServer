package org.SouthMillion.dto.item;

public record ItemMeta(
        int id,
        String name,
        ItemType itemType,
        boolean virtualItem,
        long pileLimit,     // 0 => unlimited
        long sellPrice,
        long invalidTime,   // epoch seconds or 0
        String rawTopNode,  // ví dụ "Other","gemstone","DefGift",...
        String sourceFile   // ví dụ "other.json","gift.json",...
) {}