package org.SouthMillion.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class RoleDto {
    private String roleId;
    private String roleName;
    private String serverId;
    private int level;
    private String vip;
    private Long lastLoginTime;
    private Long createTime;
    private Long exp;
    private Long cap;
    private List<RoleAttrDto> attrs;
}