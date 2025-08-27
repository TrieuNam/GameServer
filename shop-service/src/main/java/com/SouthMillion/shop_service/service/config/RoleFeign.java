package com.SouthMillion.shop_service.service.config;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="role-service", path="/api/role")
public interface RoleFeign {
    @GetMapping("/list")
    RoleDTOs.ListResp list(@RequestParam("userId") String userId);

    @PostMapping
    RoleDTOs.RoleResp create(@RequestBody RoleDTOs.CreateRoleReq req);

    @GetMapping("/{roleId}")
    RoleDTOs.RoleResp detail(@PathVariable("roleId") String roleId);
}