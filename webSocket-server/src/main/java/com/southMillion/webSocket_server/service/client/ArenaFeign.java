package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for arena-service
 * Handles PvP arena battles, rankings, rewards
 */
@FeignClient(name = "arena-service")
public interface ArenaFeign {
    
    /**
     * Get player's arena info
     */
    @GetMapping("/api/arena/player/{roleId}")
    Map<String, Object> getArenaInfo(@PathVariable("roleId") String roleId);
    
    /**
     * Get arena ranking list
     */
    @GetMapping("/api/arena/ranking")
    List<Map<String, Object>> getRanking(@RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size", defaultValue = "50") int size);
    
    /**
     * Challenge opponent in arena
     */
    @PostMapping("/api/arena/challenge")
    Map<String, Object> challenge(@RequestBody Map<String, Object> request);
    
    /**
     * Get arena opponent list
     */
    @GetMapping("/api/arena/opponents/{roleId}")
    List<Map<String, Object>> getOpponents(@PathVariable("roleId") String roleId);
    
    /**
     * Claim arena rewards
     */
    @PostMapping("/api/arena/rewards/claim")
    Map<String, Object> claimRewards(@RequestBody Map<String, Object> request);
    
    /**
     * Get arena battle history
     */
    @GetMapping("/api/arena/history/{roleId}")
    List<Map<String, Object>> getBattleHistory(@PathVariable("roleId") String roleId,
                                                @RequestParam(value = "limit", defaultValue = "10") int limit);
    
    /**
     * Buy arena challenge count
     */
    @PostMapping("/api/arena/buy-count")
    Map<String, Object> buyChallengeCoun(@RequestBody Map<String, Object> request);
}
