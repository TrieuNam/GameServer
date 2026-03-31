package com.SouthMillion.artifact_service.client;

import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for bag-service
 * Consume artifact upgrade materials
 */
@FeignClient(name = "bag-service")
public interface BagClient {
    
    /**
     * Use item from bag
     */
    @PostMapping("/api/bag/{roleId}/useitem")
    ResponseEntity<String> useItem(@PathVariable("roleId") String roleId,
                                   @RequestBody BagDTOs.UseItemReq request);
    
    /**
     * Check if player has sufficient item quantity
     */
    @GetMapping("/api/bag/{roleId}/hasitem")
    ResponseEntity<Boolean> hasItem(@PathVariable("roleId") String roleId,
                                    @RequestParam Integer itemId,
                                    @RequestParam Integer quantity);
}
