package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for territory-service
 * Handles territory wars, occupation, battles
 */
@FeignClient(name = "territory-service")
public interface TerritoryFeign {
    
    /**
     * Get all territories info
     */
    @GetMapping("/api/territory/list")
    List<Map<String, Object>> getTerritories();
    
    /**
     * Get specific territory info
     */
    @GetMapping("/api/territory/{territoryId}")
    Map<String, Object> getTerritoryInfo(@PathVariable("territoryId") Long territoryId);
    
    /**
     * Get player's territory info
     */
    @GetMapping("/api/territory/player/{roleId}")
    Map<String, Object> getPlayerTerritories(@PathVariable("roleId") String roleId);
    
    /**
     * Occupy territory
     */
    @PostMapping("/api/territory/occupy")
    Map<String, Object> occupyTerritory(@RequestBody Map<String, Object> request);
    
    /**
     * Attack territory
     */
    @PostMapping("/api/territory/attack")
    Map<String, Object> attackTerritory(@RequestBody Map<String, Object> request);
    
    /**
     * Defend territory
     */
    @PostMapping("/api/territory/defend")
    Map<String, Object> defendTerritory(@RequestBody Map<String, Object> request);
    
    /**
     * Claim territory rewards
     */
    @PostMapping("/api/territory/rewards/claim")
    Map<String, Object> claimRewards(@RequestBody Map<String, Object> request);
    
    /**
     * Get territory battle records
     */
    @GetMapping("/api/territory/battles/{territoryId}")
    List<Map<String, Object>> getBattleRecords(@PathVariable("territoryId") Long territoryId,
                                                @RequestParam(value = "limit", defaultValue = "20") int limit);
}
