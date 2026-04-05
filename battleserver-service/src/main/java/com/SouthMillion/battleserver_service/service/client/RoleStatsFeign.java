package com.SouthMillion.battleserver_service.service.client;

import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "role-service", path = "/api/other-role", contextId = "BattleRoleStatsFeign")
public interface RoleStatsFeign {

    @GetMapping("/{uid}")
    OtherRoleDTOs.OtherRoleInfo getOtherRole(@PathVariable("uid") String uid,
                                             @RequestParam(value = "roleId", required = false) String roleId);
}
