package com.SouthMillion.pet_service.service.impl;

import com.SouthMillion.pet_service.client.BagClient;
import com.SouthMillion.pet_service.exception.PetNotFoundException;
import com.SouthMillion.pet_service.exception.PetServiceException;
import com.SouthMillion.pet_service.model.entity.Pet;
import com.SouthMillion.pet_service.repository.PetRepository;
import com.SouthMillion.pet_service.service.PetGemService;
import com.SouthMillion.pet_service.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Pet Gem Service Implementation
 * Handles normal gem operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetGemServiceImpl implements PetGemService {

    private final PetRepository petRepository;
    private final PetService petService;
    private final BagClient bagClient;

    private static final int PET_GEM_SLOT_NUM = 4;

    @Override
    @Transactional
    public void inlayGem(String userId, Integer petIndex, Integer slotIndex, Integer gemItemId) {
        log.info("Inlaying gem: userId={}, petIndex={}, slot={}, gemId={}", 
            userId, petIndex, slotIndex, gemItemId);

        Pet pet = petService.getPet(userId, petIndex);
        
        if (slotIndex < 0 || slotIndex >= PET_GEM_SLOT_NUM) {
            throw new PetServiceException("Invalid gem slot index: " + slotIndex);
        }

        List<Integer> gems = new ArrayList<>(pet.getGemItemId());
        Integer currentGemId = gems.get(slotIndex);

        // If removing gem (gemItemId == 0)
        if (gemItemId == null || gemItemId == 0) {
            if (currentGemId != null && currentGemId > 0) {
                // Return gem to bag
                returnGemToBag(userId, currentGemId, 1);
                gems.set(slotIndex, 0);
            }
        } else {
            // Equipping new gem
            // Validate gem type matches slot (basic check)
            if (!canEquipGem(gemItemId, slotIndex)) {
                throw new PetServiceException("Gem type doesn't match slot");
            }
            
            // Consume gem from bag
            consumeGemFromBag(userId, gemItemId, 1);
            
            // Return old gem if exists
            if (currentGemId != null && currentGemId > 0) {
                returnGemToBag(userId, currentGemId, 1);
            }
            
            gems.set(slotIndex, gemItemId);
        }

        pet.setGemItemId(gems);
        pet.setCapability(petService.calculateCapability(userId, petIndex));
        petRepository.save(pet);

        log.info("Gem inlayed successfully: userId={}, petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
    }

    @Override
    @Transactional
    public void gemLevelUpBag(String userId, Integer itemId, List<Integer> materialItemIds) {
        log.info("Leveling up gem in bag: userId={}, itemId={}, materials={}", 
            userId, itemId, materialItemIds);

        // Validate materials
        if (materialItemIds == null || materialItemIds.isEmpty()) {
            throw new PetServiceException("No materials provided for gem upgrade");
        }
        
        // Gem upgrade formula: base gem + materials (same level) -> higher level gem
        // Simplified: itemId + 1 = upgraded gem
        int requiredMaterials = 2; // 2 same-level gems to upgrade
        if (materialItemIds.size() < requiredMaterials) {
            throw new PetServiceException("Need at least " + requiredMaterials + " materials");
        }
        
        // Consume base gem + materials from bag
        consumeGemFromBag(userId, itemId, 1);
        for (int i = 0; i < requiredMaterials; i++) {
            consumeGemFromBag(userId, materialItemIds.get(i), 1);
        }
        
        // Add upgraded gem (next level)
        Integer upgradedGemId = itemId + 1;
        returnGemToBag(userId, upgradedGemId, 1);

        log.info("Gem upgraded in bag: userId={}, oldId={}, newId={}", userId, itemId, upgradedGemId);
    }

    @Override
    @Transactional
    public void gemLevelUpPet(String userId, Integer petIndex, Integer slotIndex, 
                              List<Integer> materialItemIds) {
        log.info("Leveling up gem on pet: userId={}, petIndex={}, slot={}, materials={}", 
            userId, petIndex, slotIndex, materialItemIds);

        Pet pet = petService.getPet(userId, petIndex);
        
        if (slotIndex < 0 || slotIndex >= PET_GEM_SLOT_NUM) {
            throw new PetServiceException("Invalid gem slot: " + slotIndex);
        }

        List<Integer> gems = new ArrayList<>(pet.getGemItemId());
        Integer currentGemId = gems.get(slotIndex);
        
        if (currentGemId == null || currentGemId == 0) {
            throw new PetServiceException("No gem equipped in slot " + slotIndex);
        }

        // Validate materials
        if (materialItemIds == null || materialItemIds.isEmpty()) {
            throw new PetServiceException("No materials provided");
        }
        
        // Consume materials from bag (need 2 same-level gems as materials)
        int requiredMaterials = Math.min(2, materialItemIds.size());
        for (int i = 0; i < requiredMaterials; i++) {
            consumeGemFromBag(userId, materialItemIds.get(i), 1);
        }
        
        // Upgrade gem in place (itemId + 1 = next level)
        Integer upgradedGemId = currentGemId + 1;
        gems.set(slotIndex, upgradedGemId);
        
        pet.setGemItemId(gems);
        pet.setCapability(petService.calculateCapability(userId, petIndex));
        petRepository.save(pet);

        log.info("Gem upgraded on pet: userId={}, petIndex={}, slot={}, newGemId={}", 
            userId, petIndex, slotIndex, upgradedGemId);
    }

    @Override
    @Transactional
    public void oneKeyGemLevelUp(String userId, Integer petIndex, Integer slotIndex) {
        log.info("One-key gem level up: userId={}, petIndex={}, slot={}", 
            userId, petIndex, slotIndex);

        Pet pet = petService.getPet(userId, petIndex);
        
        if (slotIndex < 0 || slotIndex >= PET_GEM_SLOT_NUM) {
            throw new PetServiceException("Invalid gem slot: " + slotIndex);
        }

        Integer currentGemId = pet.getGemItemId().get(slotIndex);
        if (currentGemId == null || currentGemId == 0) {
            throw new PetServiceException("No gem equipped in slot " + slotIndex);
        }

        // Implement one-key upgrade algorithm (simplified version)
        // Complex algorithm from C++ (lines 1513-1805):
        // 1. Find all gems of same type in bag (via bag-service)
        // 2. Group by level
        // 3. Calculate upgrade path (2 same level -> 1 higher level)
        // 4. Consume materials automatically
        // 5. Upgrade equipped gem to highest possible level
        
        // Attempt sequential upgrades: keep upgrading until materials run out or max attempts reached
        int upgradesDone = 0;
        final int MAX_ATTEMPTS = 5;
        while (upgradesDone < MAX_ATTEMPTS) {
            try {
                Integer currentId = petService.getPet(userId, petIndex).getGemItemId().get(slotIndex);
                if (currentId == null || currentId == 0) break;
                gemLevelUpPet(userId, petIndex, slotIndex, List.of(currentId));
                upgradesDone++;
            } catch (PetServiceException e) {
                // No more materials or max level reached — stop
                log.debug("One-key gem upgrade stopped after {} levels: {}", upgradesDone, e.getMessage());
                break;
            }
        }
        log.info("One-key gem upgrade completed: userId={}, petIndex={}, slot={}, levels upgraded={}",
            userId, petIndex, slotIndex, upgradesDone);
    }

    @Override
    @Transactional
    public void dismountGem(String userId, Integer petIndex, Integer slotIndex) {
        log.info("Dismounting gem: userId={}, petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        
        inlayGem(userId, petIndex, slotIndex, 0);
    }

    @Override
    public Integer getGemSlotType(Integer slotIndex) {
        // 0=attack, 1=defense, 2=hp, 3=special
        return slotIndex % PET_GEM_SLOT_NUM;
    }

    @Override
    public boolean canEquipGem(Integer gemItemId, Integer slotIndex) {
        // Basic validation: slot index in range
        if (slotIndex < 0 || slotIndex >= PET_GEM_SLOT_NUM) {
            return false;
        }
        
        // Gem type validation (simplified)
        // In real implementation, check gem type from config
        // For now: gemItemId % 4 should match slotIndex for type matching
        int gemType = (gemItemId / 100) % 4; // Extract gem type from itemId
        int slotType = getGemSlotType(slotIndex);
        
        return true; // Allow all gems for now, config validation can be added later
    }
    
    /**
     * Helper: Consume gem from bag
     */
    private void consumeGemFromBag(String userId, Integer itemId, int quantity) {
        BagDTOs.UseItemReq request = new BagDTOs.UseItemReq();
        request.setItemId(itemId);
        request.setNum(quantity);
        
        try {
            bagClient.useItem(userId.toString(), request);
            log.debug("Consumed gem from bag: itemId={}, qty={}", itemId, quantity);
        } catch (Exception e) {
            log.error("Failed to consume gem: {}", e.getMessage());
            throw new PetServiceException("Insufficient gems in bag: " + e.getMessage());
        }
    }
    
    /**
     * Helper: Return gem to bag
     */
    private void returnGemToBag(String userId, Integer itemId, int quantity) {
        BagDTOs.GrantReq request = new BagDTOs.GrantReq();
        request.setRoleId(userId.toString());
        
        BagDTOs.GrantItem grantItem = new BagDTOs.GrantItem();
        grantItem.setItemId(itemId);
        grantItem.setNum(quantity);
        request.setItems(List.of(grantItem));
        
        try {
            bagClient.grantItems(request);
            log.debug("Returned gem to bag: itemId={}, qty={}", itemId, quantity);
        } catch (Exception e) {
            log.error("Failed to return gem: {}", e.getMessage());
            throw new PetServiceException("Failed to return gem: " + e.getMessage());
        }
    }
}
