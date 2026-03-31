package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for rune-service
 * Handles rune collection, upgrade, socketing
 */
@FeignClient(name = "rune-service")
public interface RuneFeign {
    
    /**
     * Get all runes for a user
     */
    @GetMapping("/api/rune/{userId}")
    ResponseEntity<List<Map<String, Object>>> getAllRunes(@PathVariable("userId") Long userId);
    
    /**
     * Get specific rune info
     */
    @GetMapping("/api/rune/{userId}/{runeIndex}")
    ResponseEntity<Map<String, Object>> getRune(@PathVariable("userId") Long userId,
                                                 @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Create new rune
     */
    @PostMapping("/api/rune/{userId}/create")
    ResponseEntity<Map<String, Object>> createRune(@PathVariable("userId") Long userId,
                                                    @RequestParam Integer runeId,
                                                    @RequestParam Integer quality);
    
    /**
     * Level up rune
     */
    @PostMapping("/api/rune/{userId}/{runeIndex}/levelup")
    ResponseEntity<Map<String, Object>> levelUpRune(@PathVariable("userId") Long userId,
                                                     @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Upgrade rune quality
     */
    @PostMapping("/api/rune/{userId}/{runeIndex}/quality")
    ResponseEntity<Map<String, Object>> upgradeQuality(@PathVariable("userId") Long userId,
                                                        @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Upgrade rune star
     */
    @PostMapping("/api/rune/{userId}/{runeIndex}/star")
    ResponseEntity<Map<String, Object>> upgradeStar(@PathVariable("userId") Long userId,
                                                     @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Refine rune
     */
    @PostMapping("/api/rune/{userId}/{runeIndex}/refine")
    ResponseEntity<Map<String, Object>> refineRune(@PathVariable("userId") Long userId,
                                                    @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Add exp to rune
     */
    @PostMapping("/api/rune/{userId}/{runeIndex}/exp")
    ResponseEntity<Map<String, Object>> addExp(@PathVariable("userId") Long userId,
                                                @PathVariable("runeIndex") Integer runeIndex,
                                                @RequestParam Long exp);
    
    /**
     * Equip rune to slot
     */
    @PostMapping("/api/rune/{userId}/{runeIndex}/equip")
    ResponseEntity<Map<String, Object>> equipRune(@PathVariable("userId") Long userId,
                                                   @PathVariable("runeIndex") Integer runeIndex,
                                                   @RequestParam Integer equipSlot);
    
    /**
     * Unequip rune
     */
    @DeleteMapping("/api/rune/{userId}/{runeIndex}/equip")
    ResponseEntity<Void> unequipRune(@PathVariable("userId") Long userId,
                                      @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Get all equipped runes
     */
    @GetMapping("/api/rune/{userId}/equipped")
    ResponseEntity<List<Map<String, Object>>> getEquippedRunes(@PathVariable("userId") Long userId);
    
    /**
     * Refresh sub attributes
     */
    @PostMapping("/api/rune/{userId}/{runeIndex}/refresh")
    ResponseEntity<Map<String, Object>> refreshAttributes(@PathVariable("userId") Long userId,
                                                           @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Delete rune
     */
    @DeleteMapping("/api/rune/{userId}/{runeIndex}")
    ResponseEntity<Void> deleteRune(@PathVariable("userId") Long userId,
                                     @PathVariable("runeIndex") Integer runeIndex);
    
    /**
     * Get total power of equipped runes
     */
    @GetMapping("/api/rune/{userId}/power")
    ResponseEntity<Long> getTotalPower(@PathVariable("userId") Long userId);
}
