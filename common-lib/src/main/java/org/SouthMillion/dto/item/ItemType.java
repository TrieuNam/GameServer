package org.SouthMillion.dto.item;

public enum ItemType {
    INVALID(0),
    OTHER(1),
    EQUIP(2),
    ZHAN_LI_PIN(3),
    ANGEL_EQUIP(4),
    WA_BAO(5),
    PET_ITEM(6),
    GEM(7),
    GEM_DRAWING(8),
    DEBRIS(9),
    TITLE(10),
    GIFT(11),
    INSCRIPTION(12),
    HARNESS(13),
    REMAINS(14),
    SCROLL(15),
    BUILD_BLOCK_MAP(16),
    BUILD_BLOCK_NODE(17);

    public final int code;
    ItemType(int c) { this.code = c; }

    public static ItemType fromCode(int c) {
        for (var it : values()) if (it.code == c) return it;
        return INVALID;
    }
}