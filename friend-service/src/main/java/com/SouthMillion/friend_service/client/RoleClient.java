package com.SouthMillion.friend_service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

/**
 * Feign client for role-service integration
 */
@FeignClient(name = "role-service", path = "/api/role")
public interface RoleClient {
    
    /**
     * Get role information by roleId
     * GET /api/role/{roleId}
     */
    @GetMapping("/{roleId}")
    Optional<RoleDTOs.RoleResp> getRoleInfo(@PathVariable("roleId") Long roleId);
}
