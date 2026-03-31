package com.SouthMillion.pet_service.model.enums;

import lombok.Getter;

/**
 * Pet Operation Types
 * Maps to TypeScript PET_OP_TYPE and C++ operation handlers
 */
@Getter
public enum PetOpType {
    LEVEL_UP(0, "升级"),
    GRADE_UP(1, "觉醒"),
    SKILL_LEARN(2, "学技能"),
    INLAY_GEM(3, "镶嵌普通宝石"),
    GEM_LEVEL_UP_BAG(4, "升级普通宝石(背包)"),
    GEM_LEVEL_UP_PET(5, "升级普通宝石(宠物)"),
    INLAY_TS_GEM(6, "镶嵌特殊宝石"),
    TS_GEM_LEVEL_UP(7, "特殊宝石升级"),
    TS_GEM_REFRESH(8, "特殊宝石洗练"),
    SET_FIGHT(9, "设置出战"),
    DISCARD(10, "放生"),
    SKILL_LOCK(11, "上锁技能"),
    TREASURE(12, "抽奖"),
    GRADE_UP_EVO(13, "进化"),
    OK_GEM_LEVEL_UP_PET(14, "一键升级普通宝石"),
    OK_TS_GEM_LEVEL_UP(15, "一键升级特殊宝石"),
    SEND_EVO_ATTR(16, "请求进化后属性"),
    CLOTH_UP(17, "皮肤升级"),
    CLOTH_WEAR(18, "皮肤穿戴"),
    SKILL_UNLOCK(19, "解锁技能格子");

    private final int code;
    private final String description;

    PetOpType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PetOpType fromCode(int code) {
        for (PetOpType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PetOpType code: " + code);
    }
}
