package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Setter
@Getter
public class AppearanceDTO implements Serializable {
    private Integer surfaceWeapon;   // 幻化武器
    private Integer surfaceShield;   // 幻化盾牌
    private Integer surfaceBody;     // 幻化身体
    private Integer surfaceMount;    // 幻化坐骑
    private Integer surfaceHead;     // 幻化头盔
    private Integer surfaceAngel;    // 幻化天使
}