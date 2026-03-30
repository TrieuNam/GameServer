package com.SouthMillion.mount_service.service;

import com.SouthMillion.mount_service.model.entity.MountHarness;

import java.util.List;

/**
 * Mount Harness Service Interface
 * Handles harness equipment management
 */
public interface MountHarnessService {
    
    /**
     * Get all harness items for user
     */
    List<MountHarness> getAllHarness(Long userId);
    
    /**
     * Get specific harness by index
     */
    MountHarness getHarness(Long userId, Integer harnessIndex);
    
    /**
     * Add new harness to bag
     */
    MountHarness addHarness(Long userId, Integer itemId, Integer grade);
    
    /**
     * Wear/equip harness on mount
     */
    void wearHarness(Long userId, Integer mountIndex, Integer slotIndex, Integer harnessIndex);
    
    /**
     * Remove harness from mount
     */
    void removeHarness(Long userId, Integer mountIndex, Integer slotIndex);
    
    /**
     * Decompose harness for materials/rewards
     */
    void decomposeHarness(Long userId, Integer harnessIndex);
    
    /**
     * Refresh harness entries (attributes)
     */
    MountHarness refreshEntries(Long userId, Integer harnessIndex, Integer lockFlag);
    
    /**
     * Buy harness from shop
     */
    MountHarness buyHarness(Long userId, Integer itemId, Integer buyType);
    
    /**
     * Set lock flag for entries
     */
    void setLockFlag(Long userId, Integer harnessIndex, Integer lockFlag);
    
    /**
     * Generate random entries for harness
     */
    void generateRandomEntries(MountHarness harness);
    
    /**
     * Check if bag has space
     */
    boolean hasSpace(Long userId, Integer count);
    
    /**
     * Calculate harness total stats bonus
     */
    Long calculateHarnessBonus(MountHarness harness);
}
