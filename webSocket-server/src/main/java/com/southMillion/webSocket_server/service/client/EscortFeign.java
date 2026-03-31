package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for escort-service
 * Handles escort missions (cargo/caravan escort)
 */
@FeignClient(name = "escort-service")
public interface EscortFeign {
    
    /**
     * Get player's escort info
     */
    @GetMapping("/api/escort/player/{roleId}")
    Map<String, Object> getEscortInfo(@PathVariable("roleId") String roleId);
    
    /**
     * Start new escort mission
     */
    @PostMapping("/api/escort/start")
    Map<String, Object> startEscort(@RequestBody Map<String, Object> request);
    
    /**
     * Complete escort mission
     */
    @PostMapping("/api/escort/complete")
    Map<String, Object> completeEscort(@RequestBody Map<String, Object> request);
    
    /**
     * Rob other player's escort
     */
    @PostMapping("/api/escort/rob")
    Map<String, Object> robEscort(@RequestBody Map<String, Object> request);
    
    /**
     * Get available escort targets
     */
    @GetMapping("/api/escort/targets/{roleId}")
    List<Map<String, Object>> getEscortTargets(@PathVariable("roleId") String roleId);
    
    /**
     * Speed up escort (instant complete)
     */
    @PostMapping("/api/escort/speedup")
    Map<String, Object> speedupEscort(@RequestBody Map<String, Object> request);
    
    /**
     * Get escort history/records
     */
    @GetMapping("/api/escort/history/{roleId}")
    List<Map<String, Object>> getEscortHistory(@PathVariable("roleId") String roleId,
                                                @RequestParam(value = "limit", defaultValue = "20") int limit);
}
