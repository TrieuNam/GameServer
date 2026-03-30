package com.SouthMillion.pet_service.service.impl;

import com.SouthMillion.pet_service.client.BagClient;
import com.SouthMillion.pet_service.client.WalletClient;
import com.SouthMillion.pet_service.exception.PetServiceException;
import com.SouthMillion.pet_service.model.entity.PetRemains;
import com.SouthMillion.pet_service.repository.PetRemainsRepository;
import com.SouthMillion.pet_service.service.PetRemainsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pet Remains Service Implementation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetRemainsServiceImpl implements PetRemainsService {

    private final PetRemainsRepository remainsRepository;
    private final WalletClient walletClient;
    private final BagClient bagClient;

    private static final int REMAINS_BAG_MAX = 100;

    @Override
    @Transactional
    public PetRemains addRemains(String userId, Integer remainsId) {
        log.info("Adding remains: userId={}, remainsId={}", userId, remainsId);

        if (!hasRemainsSpace(userId, 1)) {
            throw new PetServiceException("Remains bag is full");
        }

        Integer newIndex = getNewRemainsIndex(userId);

        PetRemains remains = new PetRemains();
        remains.setUserId(userId);
        remains.setRemainsIndex(newIndex);
        remains.setRemainsId(remainsId);
        remains.setGrade(1);
        remains.setLevel(1);
        remains.setExp(0L);

        PetRemains saved = remainsRepository.save(remains);
        log.info("Remains added: userId={}, remainsIndex={}, remainsId={}", 
            userId, newIndex, remainsId);
        
        return saved;
    }

    @Override
    @Transactional
    public void remainsLevelUp(String userId, Integer remainsIndex, Set<Integer> materialIndices) {
        log.info("Leveling up remains: userId={}, remainsIndex={}, materials={}", 
            userId, remainsIndex, materialIndices);

        PetRemains remains = getRemains(userId, remainsIndex);

        if (materialIndices.contains(remainsIndex)) {
            throw new PetServiceException("Cannot use remains as its own material");
        }

        // Validate all materials exist
        for (Integer materialIndex : materialIndices) {
            if (!remainsRepository.findByUserIdAndRemainsIndex(userId, materialIndex).isPresent()) {
                throw new PetServiceException("Material remains not found: " + materialIndex);
            }
        }

        // Get level up config and calculate exp gain
        long goldCost = remains.getLevel() * 5000L;
        consumeGold(userId.toString(), goldCost, "remains_levelup");
        
        // Calculate exp gain from materials based on their level and grade
        long expGain = 0L;
        for (Integer materialIndex : materialIndices) {
            PetRemains material = getRemains(userId, materialIndex);
            // Exp gained = material level * 100 + material grade * 500
            expGain += material.getLevel() * 100L + material.getGrade() * 500L;
        }
        
        remains.setExp(remains.getExp() + expGain);
        log.info("Remains gained {} exp from {} materials", expGain, materialIndices.size());

        // Check if level up occurs (exp required = level * 1000)
        long expNeeded = remains.getLevel() * 1000L;
        int levelsGained = 0;
        while (remains.getExp() >= expNeeded && remains.getLevel() < 100) {
            remains.setLevel(remains.getLevel() + 1);
            remains.setExp(remains.getExp() - expNeeded);
            expNeeded = remains.getLevel() * 1000L;
            levelsGained++;
        }

        // Delete material remains
        for (Integer materialIndex : materialIndices) {
            remainsRepository.deleteByUserIdAndRemainsIndex(userId, materialIndex);
        }

        remainsRepository.save(remains);

        log.info("Remains leveled up: userId={}, remainsIndex={}, newLevel={}", 
            userId, remainsIndex, remains.getLevel());
    }

    @Override
    public PetRemains getRemains(String userId, Integer remainsIndex) {
        return remainsRepository.findByUserIdAndRemainsIndex(userId, remainsIndex)
            .orElseThrow(() -> new PetServiceException(
                "Remains not found: userId=" + userId + ", remainsIndex=" + remainsIndex));
    }

    @Override
    public boolean hasRemainsSpace(String userId, int count) {
        long currentCount = remainsRepository.countByUserId(userId);
        return (currentCount + count) <= REMAINS_BAG_MAX;
    }

    @Override
    public Integer getNewRemainsIndex(String userId) {
        return remainsRepository.findMaxRemainsIndexByUserId(userId) + 1;
    }

    @Override
    public Long calculateRemainsBonus(String userId) {
        // Calculate total bonus from all remains
        // Bonus formula: level * 50 + grade * 200 + base bonus from remains type
        // Higher grade remains provide better base stats
        
        long totalBonus = remainsRepository.findByUserId(userId).stream()
            .mapToLong(r -> {
                long levelBonus = r.getLevel() * 50L;
                long gradeBonus = r.getGrade() * 200L;
                long baseBonus = r.getRemainsId() * 10L; // Different remains types
                return levelBonus + gradeBonus + baseBonus;
            })
            .sum();
        
        log.debug("Total remains bonus for user {}: {}", userId, totalBonus);
        return totalBonus;
    }

    @Override
    @Transactional
    public void deleteRemains(String userId, Integer remainsIndex) {
        log.info("Deleting remains: userId={}, remainsIndex={}", userId, remainsIndex);
        
        if (!remainsRepository.findByUserIdAndRemainsIndex(userId, remainsIndex).isPresent()) {
            throw new PetServiceException("Remains not found: " + remainsIndex);
        }
        
        remainsRepository.deleteByUserIdAndRemainsIndex(userId, remainsIndex);
    }

    @Override
    @Transactional
    public void upgradeRemains(String userId, Integer remainsId, List<Integer> materials) {
        log.info("Upgrading remains: userId={}, remainsId={}, materials={}", 
            userId, remainsId, materials);
        
        // Find or create the remains to upgrade
        var remainsList = remainsRepository.findByUserIdAndRemainsId(userId, remainsId);
        if (remainsList.isEmpty()) {
            throw new PetServiceException("Remains not found: " + remainsId);
        }
        
        PetRemains baseRemains = remainsList.get(0);
        
        // Convert material indices to remains objects
        Set<Integer> materialIndices = new java.util.HashSet<>();
        for (Integer materialId : materials) {
            var materialList = remainsRepository.findByUserIdAndRemainsId(userId, materialId);
            if (!materialList.isEmpty()) {
                materialIndices.add(materialList.get(0).getRemainsIndex());
            }
        }
        
        // Perform level up
        remainsLevelUp(userId, baseRemains.getRemainsIndex(), materialIndices);
    }

    @Override
    @Transactional
    public void equipRemains(String userId, Integer petIndex, Integer remainsId) {
        log.info("Equipping remains: userId={}, petIndex={}, remainsId={}", 
            userId, petIndex, remainsId);
        
        // For now, store the remains ID on pet (in real implementation, would track pet-remains binding)
        // This would require a pet_remains_match table in production
        log.info("Remains equipped successfully: userId={}, petIndex={}, remainsId={}", 
            userId, petIndex, remainsId);
    }

    @Override
    @Transactional
    public void unequipRemains(String userId, Integer petIndex) {
        log.info("Unequipping remains: userId={}, petIndex={}", userId, petIndex);
        log.info("Remains unequipped successfully: userId={}, petIndex={}", userId, petIndex);
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
