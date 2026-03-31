package com.SouthMillion.mount_service.service.impl;

import com.SouthMillion.mount_service.client.BagClient;
import com.SouthMillion.mount_service.client.WalletClient;
import com.SouthMillion.mount_service.config.MountConfigHolder;
import com.SouthMillion.mount_service.exception.MountServiceException;
import com.SouthMillion.mount_service.model.entity.Mount;
import com.SouthMillion.mount_service.publisher.MountEventPublisher;
import com.SouthMillion.mount_service.repository.MountRepository;
import com.SouthMillion.mount_service.service.MountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mount Service Implementation
 * C++ Source: gameworld/other/mount/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MountServiceImpl implements MountService {
    
    private final MountRepository mountRepository;
    private final MountConfigHolder configHolder;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    private final MountEventPublisher mountEventPublisher;
    
    // Constants
    private static final int MAX_MOUNT_LEVEL = 100;
    private static final int MAX_MOUNT_GRADE = 10;
    private static final int MAX_STAR_LEVEL = 10;
    
    @Override
    public List<Mount> getAllMounts(Long userId) {
        log.debug("Getting all mounts for user: {}", userId);
        return mountRepository.findByUserId(userId);
    }
    
    @Override
    public Mount getMount(Long userId, Integer mountIndex) {
        log.debug("Getting mount for user: {}, index: {}", userId, mountIndex);
        return mountRepository.findByUserIdAndMountIndex(userId, mountIndex)
            .orElseThrow(() -> new MountServiceException(
                "Mount not found: userId=" + userId + ", index=" + mountIndex,
                "MOUNT_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public Mount unlockMount(Long userId, Integer mountId) {
        log.info("Unlocking mount for user: {}, mountId: {}", userId, mountId);
        
        // Load mount config and check costs
        long unlockCost = configHolder.getUnlockCost(mountId);
        log.debug("Mount unlock cost: {} gold", unlockCost);
        
        // Consume gold from wallet
        consumeGold(String.valueOf(userId), unlockCost, "mount_unlock:" + mountId);
        
        // Find next available index
        List<Mount> existingMounts = mountRepository.findByUserId(userId);
        int nextIndex = existingMounts.size();
        
        // Create new mount
        Mount mount = new Mount();
        mount.setUserId(userId);
        mount.setMountIndex(nextIndex);
        mount.setMountId(mountId);
        mount.setLevel(1);
        mount.setGrade(1);
        mount.setExp(0L);
        mount.setIsActive(true);
        mount.setIsEquipped(false);
        mount.setSkinLevel(0);
        mount.setExploreProgress(0L);
        mount.setStarLevel(0);
        mount.setLockFlag(0);
        
        Mount saved = mountRepository.save(mount);
        log.info("Mount unlocked: {}", saved);
        
        // Publish Kafka event
        mountEventPublisher.publishMountUnlocked(userId, mountId, nextIndex);
        
        return saved;
    }
    
    @Override
    @Transactional
    public Mount levelUpMount(Long userId, Integer mountIndex) {
        log.info("Leveling up mount for user: {}, index: {}", userId, mountIndex);
        
        Mount mount = getMount(userId, mountIndex);
        
        if (!mount.getIsActive()) {
            throw new MountServiceException("Mount not activated", "MOUNT_NOT_ACTIVE");
        }
        
        if (mount.getLevel() >= MAX_MOUNT_LEVEL) {
            throw new MountServiceException("Mount already at max level", "MAX_LEVEL_REACHED");
        }
        
        // Get level up cost from config
        MountConfigHolder.LevelUpCost cost = configHolder.getLevelUpCost(mount.getLevel());
        log.debug("Level up cost: {} gold, {} x{}", 
            cost.getGoldCost(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        // Consume gold and materials
        consumeGold(String.valueOf(userId), cost.getGoldCost(), "mount_levelup");
        consumeMaterial(String.valueOf(userId), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        int oldLevel = mount.getLevel();
        mount.setLevel(mount.getLevel() + 1);
        mount.setExp(0L); // Reset exp after level up
        
        Mount saved = mountRepository.save(mount);
        log.info("Mount leveled up to: {}", saved.getLevel());
        
        // Publish Kafka event
        mountEventPublisher.publishMountLevelUp(
                userId,
                mount.getMountId(),
                mountIndex,
                oldLevel,
                saved.getLevel(),
                0L
        );
        
        return saved;
    }
    
    @Override
    @Transactional
    public Mount gradeUpMount(Long userId, Integer mountIndex) {
        log.info("Upgrading mount grade for user: {}, index: {}", userId, mountIndex);
        
        Mount mount = getMount(userId, mountIndex);
        
        if (mount.getGrade() >= MAX_MOUNT_GRADE) {
            throw new MountServiceException("Mount already at max grade", "MAX_GRADE_REACHED");
        }
        
        // Get grade up cost from config
        MountConfigHolder.GradeUpCost cost = configHolder.getGradeUpCost(mount.getGrade());
        log.debug("Grade up cost: {} gold, {} x{}", 
            cost.getGoldCost(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        // Consume gold and materials
        consumeGold(String.valueOf(userId), cost.getGoldCost(), "mount_gradeup");
        consumeMaterial(String.valueOf(userId), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        int oldGrade = mount.getGrade();
        mount.setGrade(mount.getGrade() + 1);
        mount.setLevel(1); // Reset to level 1 after grade up
        mount.setExp(0L);
        
        Mount saved = mountRepository.save(mount);
        log.info("Mount grade upgraded to: {}", saved.getGrade());
        
        // Publish Kafka event
        mountEventPublisher.publishMountGradeUp(
                userId,
                mount.getMountId(),
                mountIndex,
                oldGrade,
                saved.getGrade(),
                saved.getLevel()
        );
        
        return saved;
    }
    
    @Override
    @Transactional
    public void equipMount(Long userId, Integer mountIndex) {
        log.info("Equipping mount for user: {}, index: {}", userId, mountIndex);
        
        Mount mount = getMount(userId, mountIndex);
        
        if (!mount.getIsActive()) {
            throw new MountServiceException("Cannot equip inactive mount", "MOUNT_NOT_ACTIVE");
        }
        
        // Unequip all other mounts first
        mountRepository.unequipAllMounts(userId);
        
        // Equip this mount
        mount.setIsEquipped(true);
        mountRepository.save(mount);
        
        // Recalculate role capability with mount stats
        Long mountPower = calculateMountPower(mount);
        log.info("Mount equipped: {}, adding power: {}", mountIndex, mountPower);
        // Note: Role-service integration for capability update would be done via RoleClient
    }
    
    @Override
    @Transactional
    public void unequipMount(Long userId) {
        log.info("Unequipping mount for user: {}", userId);
        mountRepository.unequipAllMounts(userId);
        // Recalculate role capability without mount stats
        log.info("Mount unequipped for user: {}, removing mount power from capability", userId);
        // Note: Role-service integration for capability update would be done via RoleClient
    }
    
    @Override
    @Transactional
    public void setAppearance(Long userId, Integer mountIndex, Integer appearanceId) {
        log.info("Setting mount appearance for user: {}, index: {}, appearance: {}", 
            userId, mountIndex, appearanceId);
        
        Mount mount = getMount(userId, mountIndex);
        
        // Validate appearance ID from config
        if (appearanceId != null && appearanceId < 0) {
            throw new MountServiceException("Invalid appearance ID: " + appearanceId, "INVALID_APPEARANCE");
        }
        // Note: Full validation would check against MountConfigHolder.getMountConfig()
        
        mount.setAppearanceId(appearanceId);
        mountRepository.save(mount);
    }
    
    @Override
    @Transactional
    public Mount upgradeSkin(Long userId, Integer mountIndex) {
        log.info("Upgrading mount skin for user: {}, index: {}", userId, mountIndex);
        
        Mount mount = getMount(userId, mountIndex);
        
        // Check skin upgrade requirements and consume resources
        int currentSkinLevel = mount.getSkinLevel();
        long goldCost = (currentSkinLevel + 1) * 10000L; // 10k/20k/30k...
        int diamondCost = (currentSkinLevel + 1) * 50; // 50/100/150...
        
        consumeGold(String.valueOf(userId), goldCost, "mount_skin_upgrade");
        consumeMaterial(String.valueOf(userId), 3, diamondCost); // itemId 3 = diamond
        
        mount.setSkinLevel(mount.getSkinLevel() + 1);
        
        Mount saved = mountRepository.save(mount);
        log.info("Mount skin upgraded to level: {}", saved.getSkinLevel());
        return saved;
    }
    
    @Override
    @Transactional
    public void setSkin(Long userId, Integer mountIndex, Integer skinId) {
        log.info("Setting mount skin for user: {}, index: {}, skin: {}", 
            userId, mountIndex, skinId);
        
        Mount mount = getMount(userId, mountIndex);
        
        // Validate skin ID and check ownership
        if (skinId != null && skinId < 0) {
            throw new MountServiceException("Invalid skin ID: " + skinId, "INVALID_SKIN");
        }
        // Note: Full implementation would check bag-service for skin item ownership
        // and validate against MountConfigHolder.getSkinConfig()
        
        mount.setSkinId(skinId);
        mountRepository.save(mount);
    }
    
    @Override
    @Transactional
    public Mount explore(Long userId, Integer mountIndex, Integer exploreType) {
        log.info("Mount exploring for user: {}, index: {}, type: {}", 
            userId, mountIndex, exploreType);
        
        Mount mount = getMount(userId, mountIndex);
        
        // Check explore cost and consume
        int staminaCost = 10 * exploreType; // Type 1=10, Type 2=20, Type 3=30
        consumeMaterial(String.valueOf(userId), 5, staminaCost); // itemId 5 = stamina
        
        // Calculate explore rewards based on type
        long progressGain = 100L * exploreType; // Type affects progress gain
        int expReward = 500 * exploreType;
        int goldReward = 1000 * exploreType;
        
        // Grant rewards
        // Note: Would grant exp via role-service and gold via wallet-service
        log.info("Mount explore rewards: exp={}, gold={}, progress={}", expReward, goldReward, progressGain);
        
        mount.setExploreProgress(mount.getExploreProgress() + progressGain);
        
        Mount saved = mountRepository.save(mount);
        log.info("Mount explore progress: {}", saved.getExploreProgress());
        return saved;
    }
    
    @Override
    @Transactional
    public Mount upgradeStarLevel(Long userId, Integer mountIndex) {
        log.info("Upgrading mount star level for user: {}, index: {}", userId, mountIndex);
        
        Mount mount = getMount(userId, mountIndex);
        
        if (mount.getStarLevel() >= MAX_STAR_LEVEL) {
            throw new MountServiceException("Mount already at max star level", "MAX_STAR_REACHED");
        }
        
        // Get star upgrade cost from config
        MountConfigHolder.StarUpCost cost = configHolder.getStarUpCost(mount.getStarLevel());
        log.debug("Star upgrade cost: {} gold, {} x{}", 
            cost.getGoldCost(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        // Consume gold and materials
        consumeGold(String.valueOf(userId), cost.getGoldCost(), "mount_star_upgrade");
        consumeMaterial(String.valueOf(userId), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        mount.setStarLevel(mount.getStarLevel() + 1);
        
        Mount saved = mountRepository.save(mount);
        log.info("Mount star level upgraded to: {}", saved.getStarLevel());
        return saved;
    }
    
    @Override
    public Long calculateMountPower(Mount mount) {
        if (!mount.getIsActive()) {
            return 0L;
        }
        
        // Calculate mount power using config-based formula
        // Note: In production, formula coefficients would come from MountConfigHolder
        long basePower = mount.getLevel() * 100L;
        long gradePower = mount.getGrade() * 500L;
        long starPower = mount.getStarLevel() * 200L;
        long skinPower = mount.getSkinLevel() * 50L;
        
        long totalPower = basePower + gradePower + starPower + skinPower;
        
        log.debug("Mount power calculated: {}", totalPower);
        return totalPower;
    }
    
    @Override
    public boolean canLevelUp(Long userId, Integer mountIndex) {
        try {
            Mount mount = getMount(userId, mountIndex);
            // Check if can level up (material check would be done via bag-service)
            boolean hasLevel = mount.getLevel() < MAX_MOUNT_LEVEL;
            // Note: Full implementation: bagClient.checkItem(userId, materialItemId, quantity)
            return mount.getIsActive() && hasLevel;
        } catch (MountServiceException e) {
            return false;
        }
    }
    
    @Override
    public boolean canGradeUp(Long userId, Integer mountIndex) {
        try {
            Mount mount = getMount(userId, mountIndex);
            // Check grade up requirements (would validate via config)
            boolean hasGrade = mount.getGrade() < MAX_MOUNT_GRADE;
            // Note: Full implementation would check level requirement and materials
            return mount.getIsActive() && hasGrade;
        } catch (MountServiceException e) {
            return false;
        }
    }
    
    /**
     * Consume gold from player wallet via wallet-service
     */
    private void consumeGold(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(1L) // Gold currency itemId = 1
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(2001) // Mount system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new MountServiceException("Insufficient gold or wallet error", "WALLET_ERROR");
        }
    }
    
    /**
     * Consume material from player bag via bag-service
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
            throw new MountServiceException("Insufficient materials or bag error", "BAG_ERROR");
        }
    }
}
