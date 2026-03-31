package com.SouthMillion.pet_service.model.enums;

import lombok.Getter;

/**
 * Pet Operation Return Types
 * Maps to TypeScript PET_RET_TYPE
 */
@Getter
public enum PetRetType {
    FIGHT(0, "设置出战"),
    DISCARD(1, "放生或被消耗"),
    DISCARD_TS_GEM(2, "特殊宝石被消耗"),
    GEM_UP(3, "升级结果通知"),
    UP_EVO(4, "进化结果通知"),
    CLOTH_UP(5, "皮肤升级"),
    CLOTH_WEAR(6, "皮肤穿戴"),
    SKILL_UNLOCK(7, "解锁技能");

    private final int code;
    private final String description;

    PetRetType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PetRetType fromCode(int code) {
        for (PetRetType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PetRetType code: " + code);
    }
}
