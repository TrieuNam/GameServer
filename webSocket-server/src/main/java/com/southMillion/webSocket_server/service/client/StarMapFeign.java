package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for starmap-service
 * Handles star map/constellation system
 */
@FeignClient(name = "starmap-service")
public interface StarMapFeign {
    
    /**
     * Get player's star map info
     */
    @GetMapping("/api/starmap/player/{roleId}")
    Map<String, Object> getStarMapInfo(@PathVariable("roleId") String roleId);
    
    /**
     * Activate star node
     */
    @PostMapping("/api/starmap/activate")
    Map<String, Object> activateNode(@RequestBody Map<String, Object> request);
    
    /**
     * Upgrade star node level
     */
    @PostMapping("/api/starmap/upgrade")
    Map<String, Object> upgradeNode(@RequestBody Map<String, Object> request);
    
    /**
     * Reset star map
     */
    @PostMapping("/api/starmap/reset")
    Map<String, Object> resetStarMap(@RequestBody Map<String, Object> request);
    
    /**
     * Get star map configuration/templates
     */
    @GetMapping("/api/starmap/config")
    List<Map<String, Object>> getStarMapConfig();
    
    /**
     * Apply star map preset
     */
    @PostMapping("/api/starmap/preset")
    Map<String, Object> applyPreset(@RequestBody Map<String, Object> request);
}
