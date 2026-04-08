package com.SouthMillion.mount_service.service.impl;

import com.SouthMillion.mount_service.client.BagClient;
import com.SouthMillion.mount_service.client.RoleServiceClient;
import com.SouthMillion.mount_service.client.WalletClient;
import com.SouthMillion.mount_service.config.MountConfigHolder;
import com.SouthMillion.mount_service.exception.MountServiceException;
import com.SouthMillion.mount_service.model.entity.Mount;
import com.SouthMillion.mount_service.model.entity.MountHarness;
import com.SouthMillion.mount_service.model.MountSkill;
import com.SouthMillion.mount_service.publisher.MountEventPublisher;
import com.SouthMillion.mount_service.repository.MountHarnessRepository;
import com.SouthMillion.mount_service.repository.MountRepository;
import com.SouthMillion.mount_service.service.MountService;
import com.SouthMillion.mount_service.util.MountPowerCalculator;
import com.SouthMillion.mount_service.util.MountSkillProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mount Service Implementation
 * C++ Source: gameworld/other/mount/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MountServiceImpl implements MountService {

    private final MountRepository mountRepository;
    private final MountHarnessRepository harnessRepository;
    private final MountConfigHolder configHolder;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    private final MountEventPublisher mountEventPublisher;
    private final MountPowerCalculator powerCalculator;
    private final RoleServiceClient roleServiceClient;
    private final MountSkillProvider skillProvider;

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

        // Get equipped harness items for power calculation
        List<MountHarness> equippedHarness = getEquippedHarnessForMount(mount);

        // Calculate mount power including harness bonuses
        Long mountPower = powerCalculator.calculateTotalMountPower(mount, equippedHarness);
        log.info("Mount equipped: {}, power: {} (with {} harness)", mountIndex, mountPower, equippedHarness.size());

        // Update role capability via role-service
        updateRoleCapability(String.valueOf(userId), mountPower, "mount_equip");
    }

    @Override
    @Transactional
    public void unequipMount(Long userId) {
        log.info("Unequipping mount for user: {}", userId);

        // Get currently equipped mount to calculate power delta
        List<Mount> mounts = mountRepository.findByUserId(userId);
        Mount equippedMount = mounts.stream()
                .filter(Mount::getIsEquipped)
                .findFirst()
                .orElse(null);

        Long powerDelta = 0L;
        if (equippedMount != null) {
            List<MountHarness> equippedHarness = getEquippedHarnessForMount(equippedMount);
            powerDelta = -powerCalculator.calculateTotalMountPower(equippedMount, equippedHarness);
        }

        // Unequip all mounts
        mountRepository.unequipAllMounts(userId);

        // Update role capability (remove mount power)
        if (powerDelta != 0L) {
            updateRoleCapability(String.valueOf(userId), powerDelta, "mount_unequip");
        }

        log.info("Mount unequipped for user: {}, power delta: {}", userId, powerDelta);
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

        if (!mount.getIsActive()) {
            throw new MountServiceException("Cannot explore with inactive mount", "MOUNT_NOT_ACTIVE");
        }

        // Validate explore type (1=Quick, 2=Normal, 3=Epic)
        if (exploreType < 1 || exploreType > 3) {
            throw new MountServiceException("Invalid explore type: " + exploreType, "INVALID_EXPLORE_TYPE");
        }

        // Check explore cost and consume stamina
        int staminaCost = 10 * exploreType; // Type 1=10, Type 2=20, Type 3=30
        consumeMaterial(String.valueOf(userId), 5, staminaCost); // itemId 5 = stamina

        // Calculate explore rewards based on type and mount level/grade
        long baseProgressGain = 100L * exploreType;
        long levelBonus = mount.getLevel() * 5L; // Higher level = more progress
        long gradeBonus = mount.getGrade() * 10L; // Higher grade = more progress
        long totalProgress = baseProgressGain + levelBonus + gradeBonus;

        // Calculate rewards with mount bonuses
        int baseExpReward = 500 * exploreType;
        long baseGoldReward = 1000L * exploreType;

        // Apply mount buff bonuses if any (exp/gold bonuses from skills)
        Map<MountSkill.BuffAttribute, Long> buffs = getTotalBuffs(mount);
        Long expBonusPercent = buffs.getOrDefault(MountSkill.BuffAttribute.EXP_BONUS, 0L);
        Long goldBonusPercent = buffs.getOrDefault(MountSkill.BuffAttribute.GOLD_BONUS, 0L);

        long finalExpReward = baseExpReward + (baseExpReward * expBonusPercent / 100);
        long finalGoldReward = baseGoldReward + (baseGoldReward * goldBonusPercent / 100);

        // Grant gold reward
        addGold(String.valueOf(userId), finalGoldReward);

        // Update explore progress
        long oldProgress = mount.getExploreProgress();
        mount.setExploreProgress(oldProgress + totalProgress);

        // Check for milestone rewards (every 1000 progress)
        long oldMilestone = oldProgress / 1000;
        long newMilestone = mount.getExploreProgress() / 1000;

        if (newMilestone > oldMilestone) {
            long milestonesReached = newMilestone - oldMilestone;
            grantExploreMilestoneRewards(String.valueOf(userId), (int) milestonesReached);
            log.info("Mount reached {} new exploration milestone(s)!", milestonesReached);
        }

        Mount saved = mountRepository.save(mount);
        log.info("Mount explore complete: exp={}, gold={}, progress={} (+{})",
                finalExpReward, finalGoldReward, saved.getExploreProgress(), totalProgress);

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
        // Use MountPowerCalculator for accurate config-based power calculation
        return powerCalculator.calculateTotalMountPower(mount, List.of());
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

    @Override
    public List<MountSkill> getActiveSkills(Mount mount) {
        if (mount == null || !mount.getIsActive()) {
            return List.of();
        }

        return skillProvider.getActiveSkills(mount.getMountId(), mount.getGrade(), mount.getStarLevel());
    }

    @Override
    public Map<MountSkill.BuffAttribute, Long> getTotalBuffs(Mount mount) {
        Map<MountSkill.BuffAttribute, Long> totalBuffs = new HashMap<>();

        if (mount == null || !mount.getIsActive() || !mount.getIsEquipped()) {
            return totalBuffs;
        }

        List<MountSkill> activeSkills = getActiveSkills(mount);

        for (MountSkill skill : activeSkills) {
            MountSkill.BuffAttribute attr = skill.getBuffAttribute();
            Long currentValue = totalBuffs.getOrDefault(attr, 0L);

            // For percentage buffs, we sum them (e.g., 5% + 10% = 15%)
            // For fixed buffs, we sum them (e.g., +100 HP + +200 HP = +300 HP)
            totalBuffs.put(attr, currentValue + skill.getBuffValue());
        }

        log.debug("Mount {} total buffs: {}", mount.getMountId(), totalBuffs);
        return totalBuffs;
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

    /**
     * Add gold to player wallet via wallet-service
     */
    private void addGold(String playerId, long amount) {
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
            .reason(2002) // Mount explore reward code
            .build();

        try {
            walletClient.addCurrency(playerId, request);
            log.debug("Added {} gold for mount exploration", amount);
        } catch (Exception e) {
            // Log error but don't fail the operation
            log.error("Failed to add gold: {}", e.getMessage());
        }
    }

    /**
     * Add material to player bag via bag-service
     */
    private void addMaterial(String roleId, int itemId, int quantity) {
        if (quantity <= 0) {
            return;
        }

        try {
            BagDTOs.AddItemReq request = new BagDTOs.AddItemReq();
            request.setItemId(itemId);
            request.setNum(quantity);
            bagClient.addItem(roleId, request);
            log.debug("Added material: itemId={}, quantity={}", itemId, quantity);
        } catch (Exception e) {
            log.error("Failed to add material: {}", e.getMessage());
        }
    }

    /**
     * Grant exploration milestone rewards
     * Called when mount reaches exploration progress milestones (every 1000 points)
     */
    private void grantExploreMilestoneRewards(String userId, int milestoneCount) {
        for (int i = 0; i < milestoneCount; i++) {
            // Grant milestone rewards
            addGold(userId, 5000L); // 5000 gold per milestone
            addMaterial(userId, 1001, 5); // 5 mount exp stones
            addMaterial(userId, 1002, 2); // 2 grade stones

            // Chance for bonus rewards
            if (Math.random() < 0.3) { // 30% chance
                addMaterial(userId, 1003, 1); // 1 star stone
                log.info("Bonus star stone granted from exploration milestone!");
            }
        }

        log.info("Granted exploration milestone rewards (x{})", milestoneCount);
    }

    /**
     * Update role capability via role-service
     * Called when mount is equipped/unequipped to update player combat power
     *
     * @param roleId Player role ID
     * @param deltaValue Power change (positive for equip, negative for unequip)
     * @param reason Reason for update
     */
    private void updateRoleCapability(String roleId, Long deltaValue, String reason) {
        if (deltaValue == null || deltaValue == 0L) {
            return;
        }

        try {
            RoleServiceClient.CapabilityUpdateRequest request =
                    new RoleServiceClient.CapabilityUpdateRequest("mount", deltaValue, reason);

            roleServiceClient.updateCapability(roleId, request);
            log.info("Updated role capability: roleId={}, delta={}, reason={}", roleId, deltaValue, reason);
        } catch (Exception e) {
            // Log error but don't fail the mount operation
            // Capability update is not critical for mount equip/unequip success
            log.error("Failed to update role capability: roleId={}, delta={}, error={}",
                    roleId, deltaValue, e.getMessage());
        }
    }

    /**
     * Get equipped harness items for a mount
     * Retrieves harness details from all 4 slots if they are equipped
     *
     * @param mount The mount entity
     * @return List of equipped harness (may be empty)
     */
    private List<MountHarness> getEquippedHarnessForMount(Mount mount) {
        if (mount == null) {
            return List.of();
        }

        // Collect all equipped harness IDs from the 4 slots
        List<Integer> equippedIds = new ArrayList<>();
        if (mount.getHarnessSlot1() != null && mount.getHarnessSlot1() > 0) {
            equippedIds.add(mount.getHarnessSlot1());
        }
        if (mount.getHarnessSlot2() != null && mount.getHarnessSlot2() > 0) {
            equippedIds.add(mount.getHarnessSlot2());
        }
        if (mount.getHarnessSlot3() != null && mount.getHarnessSlot3() > 0) {
            equippedIds.add(mount.getHarnessSlot3());
        }
        if (mount.getHarnessSlot4() != null && mount.getHarnessSlot4() > 0) {
            equippedIds.add(mount.getHarnessSlot4());
        }

        if (equippedIds.isEmpty()) {
            return List.of();
        }

        // Note: Mount stores itemIds in slots, but MountHarness table stores items by harnessIndex
        // For Phase 3, we're using a simplified approach where we create virtual harness from config
        // In production, you'd need to either:
        // 1. Store full harness attributes on mount table, or
        // 2. Create separate equipped_harness table with foreign key to mount

        // For now, return empty list as harness power is calculated via config lookup
        log.debug("Mount has {} harness items equipped: {}", equippedIds.size(), equippedIds);

        // TODO Phase 3: Implement proper harness attribute storage and retrieval
        // This would require fetching harness config and creating MountHarness objects
        return List.of();
    }
}
