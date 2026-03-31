package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.model.entity.PetCloth;

/**
 * Pet Cloth Service Interface
 * Handles pet clothing/skin system
 */
public interface PetClothService {

    /**
     * Upgrade clothing level
     * @param isDiamond true to use diamonds, false to use cloth items
     */
    void clothUp(String userId, Integer clothId, boolean isDiamond);

    /**
     * Wear clothing on a pet
     */
    void clothWear(String userId, Integer petIndex, Integer clothId);

    /**
     * Get clothing by ID
     */
    PetCloth getCloth(String userId, Integer clothId);

    /**
     * Get or create clothing (ensures it exists)
     */
    PetCloth getOrCreateCloth(String userId, Integer clothId);

    /**
     * Calculate bonus attributes from clothing
     */
    Long calculateClothBonus(String userId, Integer clothId);

    /**
     * Remove clothing from pet
     */
    void unequipCloth(String userId, Integer petIndex);
}
