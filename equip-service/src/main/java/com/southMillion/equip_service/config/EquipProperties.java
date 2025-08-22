package com.southMillion.equip_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="equip")
@Data
public class EquipProperties {
    /** Level tối đa cho Fumo */
    private int fumoMaxLevel = 20;

    /** EXP cơ sở cần để lên level 1 */
    private int fumoBaseExp = 100;

    /** Mỗi level tăng thêm exp cần thiết = base + grow*(level-1) */
    private int fumoGrowExp = 50;

    /** bagType dùng cho item trang bị trong inventory (0=bag thường; 1=túi trang bị) */
    private byte equipBagType = 1;
}