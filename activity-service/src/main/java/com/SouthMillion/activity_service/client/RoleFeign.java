package com.SouthMillion.activity_service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "role-service", path = "/api/role", contextId = "activityRoleFeign")
public interface RoleFeign {

    @GetMapping("/{roleId}")
    Optional<RoleDTOs.RoleResp> detail(@PathVariable("roleId") Long roleId);
}