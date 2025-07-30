package org.SouthMillion.dto.user;

import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleInfoDTO {
    private Integer roleId;
    private String roleName;
    private Integer level;
    private Long cap;
    private Integer headPicId;
    private Integer titleId;
    private String guildName;
    private Integer knightLevel;
    private String headChar;
    private Long exp;
    private Long createTime;
}