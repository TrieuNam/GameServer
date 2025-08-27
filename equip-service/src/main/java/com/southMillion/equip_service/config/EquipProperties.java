package com.southMillion.equip_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "equip")
public class EquipProperties {

    /**
     * Loại túi dùng cho trang bị (mặc/Tháo). Dùng khi request không chỉ định bagType.
     */
    private byte equipBagType = 0;

    // ===== Fallback tính COIN khi bán (nếu meta không có sell_price) =====
    private long sellCoinBase = 0L;
    private long sellCoinPerQuality = 0L;
    private long sellCoinPerLevel = 0L;

    // ===== Fallback tính EXP khi bán (nếu meta không có sell_exp) =====
    private long sellExpBase = 0L;
    private long sellExpPerQuality = 0L;
    private long sellExpPerLevel = 0L;

    // ===== Fallback phân giải (nếu meta không có decompose_*) =====
    /** Item vật liệu rơi ra khi phân giải (fallback). */
    private int  decomposeItemId = 0;
    /** Số lượng cơ bản khi phân giải (fallback). */
    private long decomposeNumBase = 0L;
    /** Mỗi cấp trang bị cộng thêm bao nhiêu vật liệu (fallback). */
    private long decomposeNumPerLevel = 0L;

    // ===== Fumo =====
    private int fumoMaxLevel = 50;
    private int fumoBaseExp  = 100;
    private int fumoGrowExp  = 50;
}