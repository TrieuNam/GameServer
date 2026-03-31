package com.SouthMillion.angel_service.service;

import com.SouthMillion.angel_service.model.entity.Angel;

import java.util.List;

/**
 * Angel Service Interface
 */
public interface AngelService {
    
    /**
     * Get all angels for user
     */
    List<Angel> getAllAngels(Long userId);
    
    /**
     * Get specific angel
     */
    Angel getAngel(Long userId, Integer angelIndex);
    
    /**
     * Unlock/activate new angel
     */
    Angel unlockAngel(Long userId, Integer angelId);
    
    /**
     * Level up angel
     */
    Angel levelUpAngel(Long userId, Integer angelIndex);
    
    /**
     * Grade up angel
     */
    Angel gradeUpAngel(Long userId, Integer angelIndex);
    
    /**
     * Equip/display angel
     */
    void equipAngel(Long userId, Integer angelIndex);
    
    /**
     * Unequip angel
     */
    void unequipAngel(Long userId);
    
    /**
     * Upgrade star level
     */
    Angel upgradeStarLevel(Long userId, Integer angelIndex);
    
    /**
     * Upgrade angel skill
     */
    Angel upgradeSkill(Long userId, Integer angelIndex, Integer skillSlot);
    
    /**
     * Set angel appearance/skin
     */
    void setAppearance(Long userId, Integer angelIndex, Integer appearanceId);
    
    /**
     * Upgrade angel appearance level
     */
    Angel upgradeAppearance(Long userId, Integer angelIndex, Integer targetLevel);
    
    /**
     * Evolution/breakthrough
     */
    Angel evolveAngel(Long userId, Integer angelIndex);
    
    /**
     * Add blessing points
     */
    Angel addBlessing(Long userId, Integer angelIndex, Long points);
    
    /**
     * Calculate angel combat power
     */
    Long calculateAngelPower(Angel angel);
    
    /**
     * Check if can level up
     */
    boolean canLevelUp(Long userId, Integer angelIndex);
    
    /**
     * Check if can grade up
     */
    boolean canGradeUp(Long userId, Integer angelIndex);
    
    /**
     * Check if can evolve
     */
    boolean canEvolve(Long userId, Integer angelIndex);

    /**
     * Rename angel (consumes gold)
     */
    Angel renameAngel(Long userId, Integer angelIndex, String newName);
}
