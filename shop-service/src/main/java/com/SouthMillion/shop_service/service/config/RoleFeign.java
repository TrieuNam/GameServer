package com.SouthMillion.shop_service.service.config;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@FeignClient(name = "role-service", path = "/api/role", contextId = "shopRoleFeign")
public interface RoleFeign {

    /** GET /api/role/by-user/{userId} → list roles for user */
    @GetMapping("/by-user/{userId}")
    List<RoleDTOs.RoleResp> list(@PathVariable("userId") String userId);

    /** POST /api/role → create role */
    @PostMapping
    RoleDTOs.RoleResp create(@RequestBody RoleDTOs.CreateRoleReq req);

    /** GET /api/role/{roleId} → get role by Long id */
    @GetMapping("/{roleId}")
    Optional<RoleDTOs.RoleResp> detail(@PathVariable("roleId") Long roleId);
}