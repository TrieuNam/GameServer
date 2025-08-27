package org.SouthMillion.dto.item;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ItemMeta {
    private int id;
    private String name;

    // Ví dụ: EQUIP, BOX, CURRENCY, CONSUMABLE...
    private ItemType itemType;

    private boolean virtualItem;  // true nếu là tiền tệ/ảo
    private int pileLimit;        // stack tối đa
    private int sellPrice;        // giá bán
    private long invalidTime;     // hết hạn (epoch sec) nếu có

    private String rawTopNode;    // vd: "hujian", "other"...
    private String sourceFile;    // vd: "equipment", "unpack", ...
    // ... bổ sung field bạn cần
}