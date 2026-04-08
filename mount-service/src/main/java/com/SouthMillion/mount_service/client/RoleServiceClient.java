package com.SouthMillion.mount_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for role-service integration
 * Used to update player capability/stats when equipping/unequipping mounts
 */
@FeignClient(name = "role-service", path = "/api/role")
public interface RoleServiceClient {

    /**
     * Update player capability (combat power)
     * Called when mount equipment changes
     *
     * @param roleId Player role ID
     * @param request Capability update request
     */
    @PostMapping("/{roleId}/capability/update")
    void updateCapability(
        @PathVariable("roleId") String roleId,
        @RequestBody CapabilityUpdateRequest request
    );

    /**
     * Get player current capability
     *
     * @param roleId Player role ID
     * @return Current capability value
     */
    @GetMapping("/{roleId}/capability")
    CapabilityResponse getCapability(@PathVariable("roleId") String roleId);

    /**
     * Capability update request DTO
     */
    record CapabilityUpdateRequest(
        String source,      // Source of update: "mount", "equipment", etc.
        Long deltaValue,    // Change in capability (positive or negative)
        String reason       // Reason for update
    ) {}

    /**
     * Capability response DTO
     */
    record CapabilityResponse(
        Long totalCapability,
        Long baseCapability,
        Long equipmentCapability,
        Long mountCapability
    ) {}
}
