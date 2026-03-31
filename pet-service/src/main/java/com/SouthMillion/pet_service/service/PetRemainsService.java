package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.model.entity.PetRemains;

import java.util.List;
import java.util.Set;

/**
 * Pet Remains Service Interface
 * Handles pet relics/remains system
 */
public interface PetRemainsService {

    /**
     * Add a new remains to user's collection
     */
    PetRemains addRemains(String userId, Integer remainsId);

    /**
     * Level up remains using other remains as materials
     */
    void remainsLevelUp(String userId, Integer remainsIndex, Set<Integer> materialIndices);

    /**
     * Upgrade remains level
     */
    void upgradeRemains(String userId, Integer remainsId, List<Integer> materials);

    /**
     * Get remains by index
     */
    PetRemains getRemains(String userId, Integer remainsIndex);

    /**
     * Check if user has space for new remains
     */
    boolean hasRemainsSpace(String userId, int count);

    /**
     * Get new remains index
     */
    Integer getNewRemainsIndex(String userId);

    /**
     * Calculate bonus from all remains
     */
    Long calculateRemainsBonus(String userId);

    /**
     * Delete remains
     */
    void deleteRemains(String userId, Integer remainsIndex);

    /**
     * Equip remains on pet
     */
    void equipRemains(String userId, Integer petIndex, Integer remainsId);

    /**
     * Unequip remains from pet
     */
    void unequipRemains(String userId, Integer petIndex);
}

