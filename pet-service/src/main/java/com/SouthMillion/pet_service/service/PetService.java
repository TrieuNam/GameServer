package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.model.dto.*;
import com.SouthMillion.pet_service.model.entity.Pet;

import java.util.List;

/**
 * Pet Service Interface
 */
public interface PetService {

    /**
     * Get all pet information for a user (called on login)
     */
    PetAllInfoResponse getAllPetInfo(String userId);

    /**
     * Add a new pet to user's collection
     */
    Pet addPet(String userId, Integer petId);

    /**
     * Level up a pet
     * @param num number of times to level up (1 or 10)
     */
    void levelUp(String userId, Integer petIndex, Integer num);

    /**
     * Grade up (awaken) a pet
     * @param materialIndices pet indices to consume as materials
     */
    void gradeUp(String userId, Integer petIndex, List<Integer> materialIndices);

    /**
     * Evolve a pet to new type
     */
    void evolve(String userId, Integer petIndex);

    /**
     * Learn or replace a skill
     */
    void learnSkill(String userId, Integer petIndex, Integer skillIndex, Integer skillItemId);

    /**
     * Unlock a skill slot
     */
    void unlockSkill(String userId, Integer petIndex, Integer skillSeq);

    /**
     * Lock/unlock skill slots
     */
    void lockSkill(String userId, Integer petIndex, Integer lockFlag);

    /**
     * Set active fighting pet
     * @param fightIndex 0 or 1 (two pet slots)
     */
    void setFightPet(String userId, Integer petIndex, Integer fightIndex);

    /**
     * Discard (release) a pet
     */
    void discardPet(String userId, Integer petIndex);

    /**
     * Calculate pet combat capability
     */
    Long calculateCapability(String userId, Integer petIndex);

    /**
     * Recalculate all pet stats for a user
     */
    void recalculateAllStats(String userId);

    /**
     * Get a pet by index
     */
    Pet getPet(String userId, Integer petIndex);

    /**
     * Check if user has space for new pets
     */
    boolean hasSpace(String userId, int count);

    /**
     * Get new pet index for adding
     */
    Integer getNewPetIndex(String userId);

    /**
     * Get pet dungeon info
     */
    java.util.Map<String, Object> getPetDungeonInfo(String userId);

    /**
     * Start pet dungeon challenge
     */
    java.util.Map<String, Object> startPetDungeon(String userId, Integer dungeonId);

    /**
     * Claim pet dungeon reward
     */
    java.util.Map<String, Object> claimPetDungeonReward(String userId, Integer dungeonId);

    /**
     * Paid pet treasure draw used by BoxDraw popup.
     */
    java.util.Map<String, Object> drawTreasure(String userId, Integer type);
}
