package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.model.entity.Pet;

import java.util.List;

/**
 * Pet Gem Service Interface
 * Handles normal gem operations (4 slots per pet)
 */
public interface PetGemService {

    /**
     * Inlay (equip) a normal gem on pet
     * @param slotIndex 0-3 (4 slots for different gem types)
     * @param gemItemId gem item ID (0 to unequip)
     */
    void inlayGem(String userId, Integer petIndex, Integer slotIndex, Integer gemItemId);

    /**
     * Level up a gem in bag (not equipped)
     * Consumes 2+ gems of same/higher level to create 1 higher-level gem
     */
    void gemLevelUpBag(String userId, Integer itemId, List<Integer> materialItemIds);

    /**
     * Level up a gem equipped on pet
     */
    void gemLevelUpPet(String userId, Integer petIndex, Integer slotIndex, List<Integer> materialItemIds);

    /**
     * One-key gem level up (automatic upgrade)
     * Finds all available materials and upgrades to highest possible level
     */
    void oneKeyGemLevelUp(String userId, Integer petIndex, Integer slotIndex);

    /**
     * Dismount (unequip) a gem from pet
     */
    void dismountGem(String userId, Integer petIndex, Integer slotIndex);

    /**
     * Get gem slot type (0=attack, 1=defense, 2=hp, 3=special)
     */
    Integer getGemSlotType(Integer slotIndex);

    /**
     * Validate gem can be equipped in slot
     */
    boolean canEquipGem(Integer gemItemId, Integer slotIndex);
}
