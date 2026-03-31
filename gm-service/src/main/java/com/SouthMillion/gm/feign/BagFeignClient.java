package com.SouthMillion.gm.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "bag-service")
public interface BagFeignClient {
    
    @PostMapping("/api/bag/{playerId}/add")
    Map<String, Object> addItems(@PathVariable String playerId, @RequestBody Map<String, Object> request);
    
    @DeleteMapping("/api/bag/{playerId}/remove")
    Map<String, Object> removeItems(@PathVariable String playerId, @RequestBody Map<String, Object> request);
    
    @GetMapping("/api/bag/{playerId}")
    Map<String, Object> getInventory(@PathVariable String playerId);
}
