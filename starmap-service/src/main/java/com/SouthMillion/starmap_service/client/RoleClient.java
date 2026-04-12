package com.SouthMillion.starmap_service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Feign client for role-service
 * Query player level and attributes for star map requirements
 */
@FeignClient(name = "role-service", contextId = "starmapRoleQueryClient", path = "/api/role")
public interface RoleClient {

    /**
     * Get full role info — extract level, power, etc. from RoleResp.
     * GET /api/role/{roleId}
     */
    @GetMapping("/{roleId}")
    Optional<RoleDTOs.RoleResp> getRoleInfo(@PathVariable("roleId") Long roleId);

    /**
     * Get lightweight role info: name, level, fightPower.
     * GET /api/role/{roleId}/basic-info
     */
    @GetMapping("/{roleId}/basic-info")
    ResponseEntity<java.util.Map<String, Object>> getRoleBasicInfo(@PathVariable("roleId") Long roleId);
}
