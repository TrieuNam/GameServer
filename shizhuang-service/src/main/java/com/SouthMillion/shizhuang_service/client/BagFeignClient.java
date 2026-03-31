package com.SouthMillion.shizhuang_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for Bag Service
 * Handles item consumption for shizhuang operations
 */
@FeignClient(name = "bag-service", path = "/api/bag")
public interface BagFeignClient {
    
    /**
     * Consume items from bag
     */
    @PostMapping("/consume")
    Map<String, Object> consumeItems(@RequestBody Map<String, Object> req);
    
    /**
     * Check if player has items
     */
    @GetMapping("/{roleId}/has-items")
    Boolean hasItems(
            @PathVariable("roleId") String roleId,
            @RequestParam("itemId") Integer itemId,
            @RequestParam("quantity") Integer quantity
    );
    
    /**
     * Get item quantity
     */
    @GetMapping("/{roleId}/item-count")
    Integer getItemCount(
            @PathVariable("roleId") String roleId,
            @RequestParam("itemId") Integer itemId
    );
}
