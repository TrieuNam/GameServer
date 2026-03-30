package com.SouthMillion.shizhuang_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Feign Client for Role Service
 * Access player role information
 */
@FeignClient(name = "role-service", path = "/api/role", contextId = "shizhuangRoleFeignClient")
public interface RoleFeignClient {

    /**
     * Get role info (name, level, fightPower, etc.)
     * GET /api/role/{roleId}
     */
    @GetMapping("/{roleId}")
    Optional<Map<String, Object>> getRoleInfo(@PathVariable("roleId") Long roleId);

    /**
     * Get lightweight role info: name, level, fightPower.
     * GET /api/role/{roleId}/basic-info
     */
    @GetMapping("/{roleId}/basic-info")
    Map<String, Object> getRoleBasicInfo(@PathVariable("roleId") Long roleId);
}
