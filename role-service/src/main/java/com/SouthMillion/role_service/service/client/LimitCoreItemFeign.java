package com.SouthMillion.role_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client to item-service for LimitCore chip operations.
 * Uses a distinct contextId to avoid conflict with other item feign clients.
 */
@FeignClient(name = "item-service", contextId = "RoleLimitCoreItemFeign")
public interface LimitCoreItemFeign {

    @GetMapping("/api/item/{roleId}/notenough/{itemId}")
    Boolean isNotEnough(
            @PathVariable("roleId") String roleId,
            @PathVariable("itemId") int itemId,
            @RequestParam("count") int count
    );

    @PostMapping("/api/item/{roleId}/consume")
    Boolean consume(
            @PathVariable("roleId") String roleId,
            @RequestParam int itemId,
            @RequestParam int count
    );

    @PostMapping("/api/item/{roleId}/add")
    void addItem(
            @PathVariable("roleId") String roleId,
            @RequestParam int itemId,
            @RequestParam int count
    );
}
