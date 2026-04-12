package com.SouthMillion.starmap_service.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for role-service to sync player capability
 */
@FeignClient(name = "role-service", contextId = "starmapRoleCapabilityClient")
public interface RoleServiceClient {

    /**
     * Update player capability (combat power)
     *
     * @param roleId Role/Player ID
     * @param request Capability update request with delta value
     */
    @PostMapping("/api/role/{roleId}/capability/update")
    void updateCapability(
        @PathVariable("roleId") String roleId,
        @RequestBody CapabilityUpdateRequest request
    );

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CapabilityUpdateRequest {
        /**
         * Source of capability change (e.g., "starmap")
         */
        private String source;

        /**
         * Delta value to add/subtract from capability (can be negative)
         */
        private Long deltaValue;

        /**
         * Reason for the change (e.g., "star_activated", "constellation_completed")
         */
        private String reason;
    }
}
