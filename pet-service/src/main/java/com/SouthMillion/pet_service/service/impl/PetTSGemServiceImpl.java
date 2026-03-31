package com.SouthMillion.pet_service.service.impl;

import com.SouthMillion.pet_service.client.BagClient;
import com.SouthMillion.pet_service.client.WalletClient;
import com.SouthMillion.pet_service.exception.PetServiceException;
import com.SouthMillion.pet_service.model.entity.Pet;
import com.SouthMillion.pet_service.model.entity.PetTSGem;
import com.SouthMillion.pet_service.repository.PetRepository;
import com.SouthMillion.pet_service.repository.PetTSGemRepository;
import com.SouthMillion.pet_service.service.PetService;
import com.SouthMillion.pet_service.service.PetTSGemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Pet Special Gem Service Implementation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetTSGemServiceImpl implements PetTSGemService {

    private final PetTSGemRepository tsGemRepository;
    private final PetRepository petRepository;
    private final PetService petService;
    private final WalletClient walletClient;
    private final BagClient bagClient;

    private static final int TS_GEM_BAG_MAX = 100;
    private static final int PET_TS_GEM_SLOT_NUM = 2;
    private static final int PET_TS_GEM_ATTR_NUM = 4;
    
    private final Random random = new Random();

    @Override
    @Transactional
    public PetTSGem addTSGem(String userId, Integer level) {
        log.info("Adding special gem: userId={}, level={}", userId, level);

        if (!hasTSGemSpace(userId, 1)) {
            throw new PetServiceException("Special gem bag is full");
        }

        Integer newIndex = getNewTSGemIndex(userId);

        PetTSGem gem = new PetTSGem();
        gem.setUserId(userId);
        gem.setGemIndex(newIndex);
        gem.setGemLevel(level);
        gem.setPetIndex(0);
        gem.initializeAttributes();
        
        // Generate random attributes based on level
        generateRandomAttributes(gem);

        PetTSGem saved = tsGemRepository.save(gem);
        log.info("Special gem added: userId={}, gemIndex={}, level={}", 
            userId, newIndex, level);
        
        return saved;
    }

    @Override
    @Transactional
    public void inlayTSGem(String userId, Integer petIndex, Integer slotIndex, Integer gemIndex) {
        log.info("Inlaying special gem: userId={}, petIndex={}, slot={}, gemIndex={}", 
            userId, petIndex, slotIndex, gemIndex);

        Pet pet = petService.getPet(userId, petIndex);
        
        if (slotIndex < 0 || slotIndex >= PET_TS_GEM_SLOT_NUM) {
            throw new PetServiceException("Invalid special gem slot: " + slotIndex);
        }

        List<Integer> tsGemIndices = new ArrayList<>(pet.getTsGemIndex());
        Integer oldGemIndex = tsGemIndices.get(slotIndex);

        // Unequip old gem if exists
        if (oldGemIndex != null && oldGemIndex > 0) {
            PetTSGem oldGem = getTSGem(userId, oldGemIndex);
            oldGem.setPetIndex(0);
            tsGemRepository.save(oldGem);
        }

        // Equip new gem if not 0
        if (gemIndex != null && gemIndex > 0) {
            PetTSGem newGem = getTSGem(userId, gemIndex);
            
            if (newGem.getPetIndex() != 0) {
                throw new PetServiceException("Gem is already equipped on another pet");
            }
            
            newGem.setPetIndex(petIndex);
            tsGemRepository.save(newGem);
            tsGemIndices.set(slotIndex, gemIndex);
        } else {
            tsGemIndices.set(slotIndex, 0);
        }

        pet.setTsGemIndex(tsGemIndices);
        pet.setCapability(petService.calculateCapability(userId, petIndex));
        petRepository.save(pet);

        log.info("Special gem inlayed: userId={}, petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
    }

    @Override
    @Transactional
    public void tsGemLevelUp(String userId, Integer gemIndex, List<Integer> materialGemIndices) {
        log.info("Leveling up special gem: userId={}, gemIndex={}, materials={}", 
            userId, gemIndex, materialGemIndices);

        PetTSGem gem = getTSGem(userId, gemIndex);

        // Validate materials
        if (materialGemIndices.contains(gemIndex)) {
            throw new PetServiceException("Cannot use gem as its own material");
        }

        for (Integer materialIndex : materialGemIndices) {
            PetTSGem material = getTSGem(userId, materialIndex);
            
            // Validate material level (must be same or lower level)
            if (material.getGemLevel() > gem.getGemLevel()) {
                throw new PetServiceException("Material gem level too high");
            }
            
            if (material.getPetIndex() != 0) {
                throw new PetServiceException("Material gem is equipped on a pet");
            }
        }

        // Check level up requirements: need 2 gems of same level
        int requiredMaterialCount = 2;
        if (materialGemIndices.size() < requiredMaterialCount) {
            throw new PetServiceException(
                "Need " + requiredMaterialCount + " materials for level up");
        }

        // Delete material gems
        for (Integer materialIndex : materialGemIndices) {
            tsGemRepository.deleteByUserIdAndGemIndex(userId, materialIndex);
        }

        // Level up gem
        gem.setGemLevel(gem.getGemLevel() + 1);
        
        // Keep existing attributes or regenerate
        // In C++, attributes are kept but limits may increase
        tsGemRepository.save(gem);

        // Update pet capability if equipped
        if (gem.getPetIndex() != 0) {
            Pet pet = petService.getPet(userId, gem.getPetIndex());
            pet.setCapability(petService.calculateCapability(userId, gem.getPetIndex()));
            petRepository.save(pet);
        }

        log.info("Special gem leveled up: userId={}, gemIndex={}, newLevel={}", 
            userId, gemIndex, gem.getGemLevel());
    }

    @Override
    @Transactional
    public void oneKeyTSGemLevelUp(String userId, Integer gemIndex) {
        log.info("One-key special gem level up: userId={}, gemIndex={}", userId, gemIndex);

        PetTSGem gem = getTSGem(userId, gemIndex);

        // Implement automatic material finding algorithm
        // Find all unequipped gems of same level for efficient upgrade
        // Priority: use same level gems first, then lower level
        
        List<PetTSGem> sameLevel = tsGemRepository.findByUserIdAndGemLevelAndPetIndex(
            userId, gem.getGemLevel(), 0);
        sameLevel.remove(gem); // Don't include the gem itself
        
        if (sameLevel.size() < 2) {
            // Need at least 2 same level gems
            throw new PetServiceException(
                "Not enough materials for upgrade. Need 2 gems of level " + gem.getGemLevel());
        }

        // Use first 2 gems of same level as materials
        List<Integer> materials = sameLevel.stream()
            .limit(2)
            .map(PetTSGem::getGemIndex)
            .filter(idx -> !idx.equals(gemIndex))
            .toList();

        tsGemLevelUp(userId, gemIndex, materials);
    }

    @Override
    @Transactional
    public void tsGemRefresh(String userId, Integer gemIndex, Integer lockFlag) {
        log.info("Refreshing special gem attributes: userId={}, gemIndex={}, lockFlag={}", 
            userId, gemIndex, lockFlag);

        PetTSGem gem = getTSGem(userId, gemIndex);

        if (!canRefresh(userId, gemIndex)) {
            throw new PetServiceException("Gem cannot be refreshed (not all attributes filled)");
        }

        // Count locked attributes
        int lockCount = Integer.bitCount(lockFlag);

        // Calculate refresh cost based on lock count
        long goldCost = 1000L * (1 + lockCount * 2); // More locks = higher cost
        int refreshItemId = 30001; // TS Gem refresh stone
        int itemCount = 1 + lockCount; // More locks = more items
        
        // Consume refresh materials
        try {
            consumeMaterial(userId.toString(), refreshItemId, itemCount);
        } catch (Exception e) {
            // Fallback to gold if no items
            consumeGold(userId.toString(), goldCost, "ts_gem_refresh");
        }

        // Save old attributes
        List<Integer> oldAttrTypes = new ArrayList<>(gem.getAttrType());
        List<Integer> oldAttrValues = new ArrayList<>(gem.getAttrValue());

        // Generate new random attributes
        generateRandomAttributes(gem);

        // Restore locked attributes
        List<Integer> newAttrTypes = new ArrayList<>(gem.getAttrType());
        List<Integer> newAttrValues = new ArrayList<>(gem.getAttrValue());
        
        for (int i = 0; i < PET_TS_GEM_ATTR_NUM; i++) {
            if ((lockFlag & (1 << i)) != 0) {
                newAttrTypes.set(i, oldAttrTypes.get(i));
                newAttrValues.set(i, oldAttrValues.get(i));
            }
        }

        gem.setAttrType(newAttrTypes);
        gem.setAttrValue(newAttrValues);
        tsGemRepository.save(gem);

        // Update pet capability if equipped
        if (gem.getPetIndex() != 0) {
            Pet pet = petService.getPet(userId, gem.getPetIndex());
            pet.setCapability(petService.calculateCapability(userId, gem.getPetIndex()));
            petRepository.save(pet);
        }

        log.info("Special gem refreshed: userId={}, gemIndex={}", userId, gemIndex);
    }

    @Override
    @Transactional
    public void addTSGemAttr(String userId, Integer gemIndex) {
        log.info("Adding attribute to special gem: userId={}, gemIndex={}", userId, gemIndex);

        PetTSGem gem = getTSGem(userId, gemIndex);

        // Find first empty slot
        int emptySlot = -1;
        for (int i = 0; i < PET_TS_GEM_ATTR_NUM; i++) {
            if (gem.getAttrValue().get(i) == 0) {
                emptySlot = i;
                break;
            }
        }

        if (emptySlot == -1) {
            throw new PetServiceException("Gem has no empty attribute slots");
        }

        // Consume cost for adding attribute
        long goldCost = 5000L * gem.getGemLevel();
        int materialItemId = 30002; // Attribute unlock stone
        
        try {
            consumeMaterial(userId.toString(), materialItemId, 1);
        } catch (Exception e) {
            consumeGold(userId.toString(), goldCost, "ts_gem_add_attr");
        }

        // Generate new attribute for empty slot
        List<Integer> attrTypes = new ArrayList<>(gem.getAttrType());
        List<Integer> attrValues = new ArrayList<>(gem.getAttrValue());
        
        // Get attribute pool from config based on gem level
        int attributeType = random.nextInt(10) + 1; // Type 1-10
        int baseValue = 10 * gem.getGemLevel();
        int attributeValue = baseValue + random.nextInt(baseValue); // 100%-200% of base

        gem.setAttrType(attrTypes);
        gem.setAttrValue(attrValues);
        tsGemRepository.save(gem);

        log.info("Attribute added to special gem: userId={}, gemIndex={}, slot={}", 
            userId, gemIndex, emptySlot);
    }

    @Override
    @Transactional
    public void dismountTSGem(String userId, Integer petIndex, Integer slotIndex) {
        log.info("Dismounting special gem: userId={}, petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        
        inlayTSGem(userId, petIndex, slotIndex, 0);
    }

    @Override
    public boolean canRefresh(String userId, Integer gemIndex) {
        PetTSGem gem = getTSGem(userId, gemIndex);
        
        // Check if all attribute slots are filled
        for (Integer value : gem.getAttrValue()) {
            if (value == 0) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void generateRandomAttributes(PetTSGem gem) {
        // Generate attributes from config-based pool (level dependent)
        // Higher level gems get more attributes and higher values
        
        List<Integer> attrTypes = new ArrayList<>();
        List<Integer> attrValues = new ArrayList<>();
        
        // Calculate attribute count based on gem level
        // Level 1-9: 2 attrs, 10-19: 3 attrs, 20+: 4 attrs
        int attrCount = Math.min(2 + (gem.getGemLevel() / 10), PET_TS_GEM_ATTR_NUM);
        
        // Attribute value pool based on gem level
        int baseValue = 10 * gem.getGemLevel();
        int maxBonus = baseValue; // Can get 100%-200% of base value
        
        for (int i = 0; i < PET_TS_GEM_ATTR_NUM; i++) {
            if (i < attrCount) {
                // Random attribute type (1-10: HP, ATK, DEF, SPD, CRIT, etc.)
                attrTypes.add(random.nextInt(10) + 1);
                // Random value within level-based range
                attrValues.add(baseValue + random.nextInt(maxBonus));
            } else {
                attrTypes.add(0);
                attrValues.add(0);
            }
        }
        
        gem.setAttrType(attrTypes);
        gem.setAttrValue(attrValues);
    }

    @Override
    public PetTSGem getTSGem(String userId, Integer gemIndex) {
        return tsGemRepository.findByUserIdAndGemIndex(userId, gemIndex)
            .orElseThrow(() -> new PetServiceException(
                "Special gem not found: userId=" + userId + ", gemIndex=" + gemIndex));
    }

    @Override
    public boolean hasTSGemSpace(String userId, int count) {
        long currentCount = tsGemRepository.countByUserId(userId);
        return (currentCount + count) <= TS_GEM_BAG_MAX;
    }

    @Override
    public Integer getNewTSGemIndex(String userId) {
        return tsGemRepository.findMaxGemIndexByUserId(userId) + 1;
    }
    
    /**
     * Consume gold from player wallet
     */
    private void consumeGold(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(1L)
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(2100)
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new PetServiceException("Insufficient gold: " + e.getMessage());
        }
    }
    
    /**
     * Consume material from player bag
     */
    private void consumeMaterial(String roleId, int itemId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        
        BagDTOs.UseItemReq request = new BagDTOs.UseItemReq();
        request.setItemId(itemId);
        request.setNum(quantity);
        
        try {
            bagClient.useItem(roleId, request);
            log.debug("Consumed material: itemId={}, quantity={}", itemId, quantity);
        } catch (Exception e) {
            log.error("Failed to consume material: {}", e.getMessage());
            throw new PetServiceException("Insufficient materials: " + e.getMessage());
        }
    }
}
