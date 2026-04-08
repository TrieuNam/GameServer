package com.SouthMillion.rune_service.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for role-service
 * Used to update player capability when runes are equipped/unequipped
 */
@FeignClient(name = "role-service")
public interface RoleServiceClient {

    /**
     * Update player capability (combat power)
     * Called when rune power changes (equip/unequip/upgrade)
     *
     * @param roleId Player role ID
     * @param request Capability update request
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
         * Source of capability change (e.g., "rune")
         */
        private String source;

        /**
         * Delta value to add/subtract from player capability
         * Positive for power increase, negative for decrease
         */
        private Long deltaValue;

        /**
         * Reason for update (e.g., "rune_equip", "rune_levelup")
         */
        private String reason;
    }
}
