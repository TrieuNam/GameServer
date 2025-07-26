package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Setter
@Getter
public class RoleInfoDTO implements Serializable {
    private Integer roleId;          // 角色id
    private String name;             // 角色名
    private Integer level;           // 等级
    private Long cap;                // 战力
    private Integer headPicId;       // 头像ID
    private Integer titleId;         // 称号ID
    private String guildName;        // 公会名
    private Integer knightLevel;     // 骑士团手册等级
    private String headChar;         // 微信头像地址
}