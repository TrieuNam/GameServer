package com.SouthMillion.gameworld_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for role-service integration
 * Used to fetch player level for nearby-player info
 */
@FeignClient(name = "role-service", path = "/api/role")
public interface RoleServiceClient {

    /**
     * Get basic role info (includes level, power, etc.)
     * GET /api/role/{roleId}
     */
    @GetMapping("/{roleId}")
    Map<String, Object> getRoleInfo(@PathVariable("roleId") Long roleId);
}
