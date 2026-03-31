package com.SouthMillion.artifact_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for role-service
 * Query player attributes and level for artifact requirements
 */
@FeignClient(name = "role-service")
public interface RoleClient {
    
    /**
     * Get player level for unlock validation
     */
    @GetMapping("/api/role/{roleId}/level")
    ResponseEntity<Integer> getPlayerLevel(@PathVariable("roleId") String roleId);
    
    /**
     * Get player combat power
     */
    @GetMapping("/api/role/{roleId}/power")
    ResponseEntity<Long> getPlayerPower(@PathVariable("roleId") String roleId);
}
