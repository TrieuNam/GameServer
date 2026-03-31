package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.model.entity.PetTSGem;

import java.util.List;

/**
 * Pet Special Gem Service Interface  
 * Handles special gems (TS Gem) with random attributes
 */
public interface PetTSGemService {

    /**
     * Add a new special gem to user's collection
     */
    PetTSGem addTSGem(String userId, Integer level);

    /**
     * Inlay (equip) a special gem on pet
     * @param slotIndex 0-1 (2 slots for special gems)
     */
    void inlayTSGem(String userId, Integer petIndex, Integer slotIndex, Integer gemIndex);

    /**
     * Level up a special gem
     * Requires 2+ special gems as materials
     */
    void tsGemLevelUp(String userId, Integer gemIndex, List<Integer> materialGemIndices);

    /**
     * One-key special gem level up (automatic)
     */
    void oneKeyTSGemLevelUp(String userId, Integer gemIndex);

    /**
     * Refresh special gem attributes
     * @param lockFlag bitmask indicating which attributes to keep (locked)
     */
    void tsGemRefresh(String userId, Integer gemIndex, Integer lockFlag);

    /**
     * Add an attribute to special gem
     * Used when gem has empty attribute slots
     */
    void addTSGemAttr(String userId, Integer gemIndex);

    /**
     * Dismount (unequip) special gem from pet
     */
    void dismountTSGem(String userId, Integer petIndex, Integer slotIndex);

    /**
     * Check if special gem can be refreshed
     * (must have all attribute slots filled)
     */
    boolean canRefresh(String userId, Integer gemIndex);

    /**
     * Generate random attributes for special gem based on level
     */
    void generateRandomAttributes(PetTSGem gem);

    /**
     * Get special gem by index
     */
    PetTSGem getTSGem(String userId, Integer gemIndex);

    /**
     * Check if user has space for new special gems
     */
    boolean hasTSGemSpace(String userId, int count);

    /**
     * Get new special gem index
     */
    Integer getNewTSGemIndex(String userId);
}
