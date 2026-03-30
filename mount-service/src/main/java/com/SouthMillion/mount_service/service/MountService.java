package com.SouthMillion.mount_service.service;

import com.SouthMillion.mount_service.model.entity.Mount;

import java.util.List;

/**
 * Mount Service Interface
 * Main business logic for mount operations
 */
public interface MountService {
    
    /**
     * Get all mounts for user
     */
    List<Mount> getAllMounts(Long userId);
    
    /**
     * Get specific mount by index
     */
    Mount getMount(Long userId, Integer mountIndex);
    
    /**
     * Unlock/activate a new mount
     */
    Mount unlockMount(Long userId, Integer mountId);
    
    /**
     * Level up mount (using items/materials)
     */
    Mount levelUpMount(Long userId, Integer mountIndex);
    
    /**
     * Grade up mount (quality upgrade)
     */
    Mount gradeUpMount(Long userId, Integer mountIndex);
    
    /**
     * Equip/ride a mount
     */
    void equipMount(Long userId, Integer mountIndex);
    
    /**
     * Unequip current mount
     */
    void unequipMount(Long userId);
    
    /**
     * Set mount appearance (cosmetic)
     */
    void setAppearance(Long userId, Integer mountIndex, Integer appearanceId);
    
    /**
     * Upgrade mount skin
     */
    Mount upgradeSkin(Long userId, Integer mountIndex);
    
    /**
     * Set mount skin
     */
    void setSkin(Long userId, Integer mountIndex, Integer skinId);
    
    /**
     * Mount exploration (gain progress/rewards)
     */
    Mount explore(Long userId, Integer mountIndex, Integer exploreType);
    
    /**
     * Star level upgrade
     */
    Mount upgradeStarLevel(Long userId, Integer mountIndex);
    
    /**
     * Calculate mount combat power/capability
     */
    Long calculateMountPower(Mount mount);
    
    /**
     * Check if mount can be upgraded
     */
    boolean canLevelUp(Long userId, Integer mountIndex);
    
    /**
     * Check if mount can grade up
     */
    boolean canGradeUp(Long userId, Integer mountIndex);
}
