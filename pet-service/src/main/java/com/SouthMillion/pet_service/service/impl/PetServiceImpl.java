package com.SouthMillion.pet_service.service.impl;

import com.SouthMillion.pet_service.client.BagClient;
import com.SouthMillion.pet_service.client.WalletClient;
import com.SouthMillion.pet_service.exception.*;
import com.SouthMillion.pet_service.model.dto.*;
import com.SouthMillion.pet_service.model.entity.*;
import com.SouthMillion.pet_service.repository.*;
import com.SouthMillion.pet_service.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pet Service Implementation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final PetTSGemRepository tsGemRepository;
    private final PetClothRepository clothRepository;
    private final PetRemainsRepository remainsRepository;
    private final PetFightIndexRepository fightIndexRepository;
    private final PetDungeonRepository petDungeonRepository;
    private final BagClient bagClient;
    private final WalletClient walletClient;

    private static final int PET_BAG_MAX = 100;
    private static final int GOLD_COIN_ITEM_ID = 1;

    @Override
    @Transactional(readOnly = true)
    public PetAllInfoResponse getAllPetInfo(String userId) {
        log.debug("Getting all pet info for user: {}", userId);

        // Get fight indices
        PetFightIndex fightIndex = fightIndexRepository.findByUserId(userId)
            .orElse(new PetFightIndex(userId, 0, 0, null));

        // Get all pets
        List<Pet> pets = petRepository.findByUserId(userId);
        List<PetDataDTO> petDataList = pets.stream()
            .map(this::convertToPetDataDTO)
            .collect(Collectors.toList());

        // Get all special gems
        List<PetTSGem> tsGems = tsGemRepository.findByUserId(userId);
        List<TSGemDataDTO> tsGemDataList = tsGems.stream()
            .map(this::convertToTSGemDataDTO)
            .collect(Collectors.toList());

        // Get all clothing
        List<PetCloth> clothes = clothRepository.findByUserId(userId);
        List<ClothDataDTO> clothDataList = clothes.stream()
            .map(this::convertToClothDataDTO)
            .collect(Collectors.toList());

        PetAllInfoResponse response = new PetAllInfoResponse();
        response.setFightPetIndex(List.of(fightIndex.getFightPetIndex(), fightIndex.getFightPetIndex2()));
        response.setPetList(petDataList);
        response.setTsGemList(tsGemDataList);
        response.setClothList(clothDataList);

        return response;
    }

    @Override
    @Transactional
    public Pet addPet(String userId, Integer petId) {
        log.info("Adding pet for user: {}, petId: {}", userId, petId);

        if (!hasSpace(userId, 1)) {
            throw new PetBagFullException(userId, 
                (int) petRepository.countByUserId(userId), PET_BAG_MAX);
        }

        Integer newIndex = getNewPetIndex(userId);
        
        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setPetIndex(newIndex);
        pet.setPetId(petId);
        pet.setLevel(1);
        pet.setExp(0L);
        pet.setOrder(1);
        
        // Initialize default values
        pet.initializeSkills(List.of(1, 1, -1, -1, -1, -1)); // First 2 unlocked
        pet.initializeGems();
        pet.initializeTSGems();
        pet.setSkillLockFlag(0);
        pet.setClothId(0);
        pet.setCapability(calculateCapability(userId, newIndex));

        Pet savedPet = petRepository.save(pet);
        log.info("Pet added successfully: userId={}, petIndex={}, petId={}", 
            userId, newIndex, petId);
        
        return savedPet;
    }

    @Override
    @Transactional
    public void levelUp(String userId, Integer petIndex, Integer num) {
        log.info("Leveling up pet: userId={}, petIndex={}, num={}", userId, petIndex, num);

        Pet pet = getPet(userId, petIndex);
        
        // Check role level via Feign client
        Integer roleLevel = 300; // Default
        try {
            // roleLevel = roleClient.getRoleLevel(userId);
            // For now use default until role-service client is integrated
        } catch (Exception e) {
            log.warn("Failed to get role level, using default: {}", e.getMessage());
        }
        
        int targetLevel = pet.getLevel() + num;
        if (targetLevel > roleLevel) {
            throw new PetLevelExceedRoleLevelException(targetLevel, roleLevel);
        }

        // Calculate and consume gold
        long goldCost = calculateLevelUpCost(pet.getLevel(), num);
        consumeGold(userId.toString(), goldCost, "pet_levelup");

        pet.setLevel(targetLevel);
        pet.setCapability(calculateCapability(userId, petIndex));
        
        petRepository.save(pet);
        log.info("Pet leveled up: userId={}, petIndex={}, newLevel={}", 
            userId, petIndex, targetLevel);
    }

    @Override
    @Transactional
    public void gradeUp(String userId, Integer petIndex, List<Integer> materialIndices) {
        log.info("Grading up pet: userId={}, petIndex={}, materials={}", 
            userId, petIndex, materialIndices);

        Pet pet = getPet(userId, petIndex);
        
        // Validate materials exist and are different from main pet
        if (materialIndices.contains(petIndex)) {
            throw new PetServiceException("Cannot use pet as its own material");
        }

        for (Integer materialIndex : materialIndices) {
            if (!petRepository.existsByUserIdAndPetIndex(userId, materialIndex)) {
                throw new PetNotFoundException(userId, materialIndex);
            }
        }

        // Calculate grade up cost
        long goldCost = pet.getOrder() * 10000L;
        int materialItemId = 20001; // Pet grade stone
        int materialCount = pet.getOrder() * 5;
        
        // Consume resources
        consumeGold(userId.toString(), goldCost, "pet_gradeup");
        consumeMaterial(userId.toString(), materialItemId, materialCount);

        pet.setOrder(pet.getOrder() + 1);
        pet.setCapability(calculateCapability(userId, petIndex));
        
        // Delete material pets
        for (Integer materialIndex : materialIndices) {
            petRepository.deleteByUserIdAndPetIndex(userId, materialIndex);
        }

        petRepository.save(pet);
        log.info("Pet graded up: userId={}, petIndex={}, newOrder={}", 
            userId, petIndex, pet.getOrder());
    }

    @Override
    @Transactional
    public void evolve(String userId, Integer petIndex) {
        log.info("Evolving pet: userId={}, petIndex={}", userId, petIndex);

        Pet pet = getPet(userId, petIndex);
        
        // Check evolution requirements
        if (pet.getLevel() < 200) {
            throw new PetServiceException("Pet level must be at least 200 to evolve");
        }

        // Get evolution config and consume materials
        long goldCost = 100000L;
        int materialItemId = 20002; // Evolution stone
        int materialCount = 10;
        
        consumeGold(userId.toString(), goldCost, "pet_evolution");
        consumeMaterial(userId.toString(), materialItemId, materialCount);
        
        // Change pet_id to evolved form (config-based transformation)
        Integer newPetId = pet.getPetId() + 1000; // Evolved pet ID = base + 1000
        pet.setPetId(newPetId);
        pet.setLevel(pet.getLevel() + 10); // Bonus levels
        pet.setCapability(calculateCapability(userId, petIndex));

        petRepository.save(pet);
        log.info("Pet evolved: userId={}, petIndex={}, newPetId={}", 
            userId, petIndex, newPetId);
    }

    @Override
    @Transactional
    public void learnSkill(String userId, Integer petIndex, Integer skillIndex, Integer skillItemId) {
        log.info("Learning skill: userId={}, petIndex={}, skillIndex={}, skillItemId={}", 
            userId, petIndex, skillIndex, skillItemId);

        Pet pet = getPet(userId, petIndex);
        
        // Validate and consume skill book
        consumeMaterial(userId.toString(), skillItemId, 1);

        List<Integer> skills = new ArrayList<>(pet.getSkillList());
        if (skillIndex >= 0 && skillIndex < skills.size()) {
            skills.set(skillIndex, skillItemId);
            pet.setSkillList(skills);
            petRepository.save(pet);
        } else {
            throw new PetServiceException("Invalid skill index: " + skillIndex);
        }

        log.info("Skill learned: userId={}, petIndex={}, skillIndex={}", 
            userId, petIndex, skillIndex);
    }

    @Override
    @Transactional
    public void unlockSkill(String userId, Integer petIndex, Integer skillSeq) {
        log.info("Unlocking skill slot: userId={}, petIndex={}, seq={}", 
            userId, petIndex, skillSeq);

        Pet pet = getPet(userId, petIndex);
        
        List<Integer> skills = new ArrayList<>(pet.getSkillList());
        if (skillSeq >= 0 && skillSeq < skills.size() && skills.get(skillSeq) == -1) {
            // Consume unlock cost (diamonds)
            long unlockCost = 1000L * (skillSeq + 1); // Progressive cost
            consumeDiamonds(userId.toString(), unlockCost, "pet_skill_unlock");
            
            skills.set(skillSeq, 0); // 0 = unlocked but empty
            pet.setSkillList(skills);
            petRepository.save(pet);
        } else {
            throw new PetServiceException("Invalid skill slot or already unlocked");
        }
    }

    @Override
    @Transactional
    public void lockSkill(String userId, Integer petIndex, Integer lockFlag) {
        Pet pet = getPet(userId, petIndex);
        pet.setSkillLockFlag(lockFlag);
        petRepository.save(pet);
    }

    @Override
    @Transactional
    public void setFightPet(String userId, Integer petIndex, Integer fightIndex) {
        log.info("Setting fight pet: userId={}, petIndex={}, fightIndex={}", 
            userId, petIndex, fightIndex);

        // Validate pet exists if not 0
        if (petIndex != 0 && !petRepository.existsByUserIdAndPetIndex(userId, petIndex)) {
            throw new PetNotFoundException(userId, petIndex);
        }

        PetFightIndex fightIndexEntity = fightIndexRepository.findByUserId(userId)
            .orElse(new PetFightIndex(userId, 0, 0, null));

        if (fightIndex == 0) {
            fightIndexEntity.setFightPetIndex(petIndex);
        } else if (fightIndex == 1) {
            fightIndexEntity.setFightPetIndex2(petIndex);
        } else {
            throw new PetServiceException("Invalid fight index: " + fightIndex);
        }

        fightIndexRepository.save(fightIndexEntity);
        log.info("Fight pet set: userId={}, petIndex={}, fightIndex={}", 
            userId, petIndex, fightIndex);
    }

    @Override
    @Transactional
    public void discardPet(String userId, Integer petIndex) {
        log.info("Discarding pet: userId={}, petIndex={}", userId, petIndex);

        if (!petRepository.existsByUserIdAndPetIndex(userId, petIndex)) {
            throw new PetNotFoundException(userId, petIndex);
        }

        // Remove from fight index if active
        fightIndexRepository.findByUserId(userId).ifPresent(fightIndex -> {
            if (fightIndex.getFightPetIndex().equals(petIndex)) {
                fightIndex.setFightPetIndex(0);
            }
            if (fightIndex.getFightPetIndex2().equals(petIndex)) {
                fightIndex.setFightPetIndex2(0);
            }
            fightIndexRepository.save(fightIndex);
        });

        petRepository.deleteByUserIdAndPetIndex(userId, petIndex);
        log.info("Pet discarded: userId={}, petIndex={}", userId, petIndex);
    }

    @Override
    public Long calculateCapability(String userId, Integer petIndex) {
        Pet pet = getPet(userId, petIndex);
        
        // Base capability from pet template
        long basePower = pet.getPetId() * 100L;
        
        // Level contribution
        long levelPower = pet.getLevel() * 50L;
        
        // Order/grade contribution
        long orderPower = pet.getOrder() * 1000L;
        
        // Gem bonuses - calculate from equipped gems
        long gemPower = calculateGemPower(pet);
        
        // TS Gem bonuses - calculate from equipped TS gems and attributes
        long tsGemPower = calculateTSGemPower(pet);
        
        // Cloth bonus
        long clothPower = pet.getClothId() > 0 ? 500L : 0L;
        
        // Total capability
        return basePower + levelPower + orderPower + gemPower + tsGemPower + clothPower;
    }

    /**
     * Calculate total power contribution from normal gems
     * Each gem contributes power based on its level
     */
    private long calculateGemPower(Pet pet) {
        long power = 0L;
        
        // Gems are stored in gemPos1-4 with format "gemId:level"
        power += parseGemLevel(pet.getGemPos1() != null ? pet.getGemPos1().toString() : null) * 100L;
        power += parseGemLevel(pet.getGemPos2() != null ? pet.getGemPos2().toString() : null) * 100L;
        power += parseGemLevel(pet.getGemPos3() != null ? pet.getGemPos3().toString() : null) * 100L;
        power += parseGemLevel(pet.getGemPos4() != null ? pet.getGemPos4().toString() : null) * 100L;
        
        return power;
    }

    /**
     * Calculate total power contribution from TS gems
     * Each equipped TS gem contributes based on its attributes
     */
    private long calculateTSGemPower(Pet pet) {
        long power = 0L;
        
        // Count equipped TS gems (tsGemPos1 and tsGemPos2)
        if (pet.getTsGemPos1() != null && pet.getTsGemPos1() > 0) {
            // Each TS gem contributes base power plus attribute bonuses
            power += 500L; // Base TS gem power
        }
        if (pet.getTsGemPos2() != null && pet.getTsGemPos2() > 0) {
            power += 500L;
        }
        
        return power;
    }

    /**
     * Parse gem level from gem position string (format: "gemId:level")
     */
    private int parseGemLevel(String gemPos) {
        if (gemPos == null || gemPos.isEmpty() || "0".equals(gemPos)) {
            return 0;
        }
        
        String[] parts = gemPos.split(":");
        if (parts.length == 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse gem level from: {}", gemPos);
                return 0;
            }
        }
        return 0;
    }

    @Override
    @Transactional
    public void recalculateAllStats(String userId) {
        List<Pet> pets = petRepository.findByUserId(userId);
        for (Pet pet : pets) {
            pet.setCapability(calculateCapability(userId, pet.getPetIndex()));
        }
        petRepository.saveAll(pets);
    }

    @Override
    @Transactional(readOnly = true)
    public Pet getPet(String userId, Integer petIndex) {
        return petRepository.findByUserIdAndPetIndex(userId, petIndex)
            .orElseThrow(() -> new PetNotFoundException(userId, petIndex));
    }

    @Override
    public boolean hasSpace(String userId, int count) {
        long currentCount = petRepository.countByUserId(userId);
        return (currentCount + count) <= PET_BAG_MAX;
    }

    @Override
    public Integer getNewPetIndex(String userId) {
        return petRepository.findMaxPetIndexByUserId(userId) + 1;
    }

    // Helper methods for DTO conversion

    private PetDataDTO convertToPetDataDTO(Pet pet) {
        PetDataDTO dto = new PetDataDTO();
        dto.setPetIndex(pet.getPetIndex());
        dto.setPetId(pet.getPetId());
        dto.setPetLevel(pet.getLevel());
        dto.setPetExp(pet.getExp());
        dto.setPetOrder(pet.getOrder());
        dto.setSkillList(pet.getSkillList());
        dto.setGemItemId(pet.getGemItemId());
        dto.setTsGemIndex(pet.getTsGemIndex());
        dto.setCapability(pet.getCapability());
        dto.setSkillLockFlag(pet.getSkillLockFlag());
        
        // Calculate attributes from pet config and level
        List<Integer> attrList = new ArrayList<>();
        // Base attributes (type, value pairs)
        attrList.add(1); // HP type
        attrList.add((int)(pet.getLevel() * 100)); // HP value
        attrList.add(2); // ATK type
        attrList.add((int)(pet.getLevel() * 10)); // ATK value
        attrList.add(3); // DEF type
        attrList.add((int)(pet.getLevel() * 8)); // DEF value
        dto.setAttrList(attrList);
        
        return dto;
    }

    private TSGemDataDTO convertToTSGemDataDTO(PetTSGem gem) {
        TSGemDataDTO dto = new TSGemDataDTO();
        dto.setGemIndex(gem.getGemIndex());
        dto.setGemLevel(gem.getGemLevel());
        dto.setPetIndex(gem.getPetIndex());
        dto.setAttrType(gem.getAttrType());
        dto.setAttrValue(gem.getAttrValue());
        return dto;
    }

    private ClothDataDTO convertToClothDataDTO(PetCloth cloth) {
        ClothDataDTO dto = new ClothDataDTO();
        dto.setItemId(cloth.getClothId());
        dto.setLevel(cloth.getLevel());
        dto.setPetIndex(cloth.getPetIndex());
        return dto;
    }
    
    /**
     * Calculate level up cost based on current level and number of levels
     */
    private long calculateLevelUpCost(int currentLevel, int numLevels) {
        long totalCost = 0;
        for (int i = 0; i < numLevels; i++) {
            totalCost += (currentLevel + i) * 50L; // 50 gold per level, scaled by level
        }
        return totalCost;
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
            .itemId(1L) // Gold
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(2100) // Pet system reason code
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
     * Consume diamonds from player wallet
     */
    private void consumeDiamonds(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(3L) // Diamonds
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(2101) // Pet system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} diamonds for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume diamonds: {}", e.getMessage());
            throw new PetServiceException("Insufficient diamonds: " + e.getMessage());
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

    // ============================================
    // PET DUNGEON (宠物副本)
    // ============================================

    @Override
    public java.util.Map<String, Object> getPetDungeonInfo(String userId) {
        log.info("[PetFb] getPetDungeonInfo - userId={}", userId);
        com.SouthMillion.pet_service.model.entity.PetDungeon dungeon = getOrCreatePetDungeon(userId);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("passLevel", dungeon.getPassLevel());
        result.put("fetchFlag", dungeon.getFetchFlag());
        return result;
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> startPetDungeon(String userId, Integer dungeonId) {
        log.info("[PetFb] startPetDungeon - userId={}, dungeonId={}", userId, dungeonId);
        com.SouthMillion.pet_service.model.entity.PetDungeon dungeon = getOrCreatePetDungeon(userId);
        // Auto-clear: if dungeonId is the next level, advance passLevel
        if (dungeonId != null && dungeonId == dungeon.getPassLevel() + 1) {
            dungeon.setPassLevel(dungeonId);
            petDungeonRepository.save(dungeon);
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("passLevel", dungeon.getPassLevel());
        result.put("fetchFlag", dungeon.getFetchFlag());
        return result;
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> claimPetDungeonReward(String userId, Integer dungeonId) {
        log.info("[PetFb] claimPetDungeonReward - userId={}, dungeonId={}", userId, dungeonId);
        com.SouthMillion.pet_service.model.entity.PetDungeon dungeon = getOrCreatePetDungeon(userId);
        boolean claimed = false;
        if (dungeonId != null && dungeonId >= 1 && dungeonId <= dungeon.getPassLevel()) {
            long bit = 1L << (dungeonId - 1);
            if ((dungeon.getFetchFlag() & bit) == 0) {
                dungeon.setFetchFlag(dungeon.getFetchFlag() | bit);
                petDungeonRepository.save(dungeon);
                claimed = true;
            }
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", claimed);
        result.put("passLevel", dungeon.getPassLevel());
        result.put("fetchFlag", dungeon.getFetchFlag());
        return result;
    }

    private com.SouthMillion.pet_service.model.entity.PetDungeon getOrCreatePetDungeon(String userId) {
        return petDungeonRepository.findByUserId(userId).orElseGet(() ->
                petDungeonRepository.save(
                        com.SouthMillion.pet_service.model.entity.PetDungeon.builder()
                                .userId(userId).passLevel(0).fetchFlag(0L).build()));
    }
}
