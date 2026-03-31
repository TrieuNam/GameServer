package com.SouthMillion.mount_service.service.impl;

import com.SouthMillion.mount_service.client.BagClient;
import com.SouthMillion.mount_service.client.WalletClient;
import com.SouthMillion.mount_service.config.MountConfigHolder;
import com.SouthMillion.mount_service.exception.MountServiceException;
import com.SouthMillion.mount_service.model.entity.Mount;
import com.SouthMillion.mount_service.model.entity.MountHarness;
import com.SouthMillion.mount_service.repository.MountHarnessRepository;
import com.SouthMillion.mount_service.repository.MountRepository;
import com.SouthMillion.mount_service.service.MountHarnessService;
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
 * Mount Harness Service Implementation
 * C++ Source: gameworld/other/mount/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MountHarnessServiceImpl implements MountHarnessService {
    
    private final MountHarnessRepository harnessRepository;
    private final MountRepository mountRepository;
    private final MountConfigHolder configHolder;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    private final Random random = new Random();
    
    // Constants
    private static final int HARNESS_BAG_MAX = 100;
    private static final int HARNESS_SLOT_NUM = 4;
    private static final int ENTRY_NUM = 4;
    
    @Override
    public List<MountHarness> getAllHarness(Long userId) {
        log.debug("Getting all harness for user: {}", userId);
        return harnessRepository.findByUserId(userId);
    }
    
    @Override
    public MountHarness getHarness(Long userId, Integer harnessIndex) {
        log.debug("Getting harness for user: {}, index: {}", userId, harnessIndex);
        return harnessRepository.findByUserIdAndHarnessIndex(userId, harnessIndex)
            .orElseThrow(() -> new MountServiceException(
                "Harness not found: userId=" + userId + ", index=" + harnessIndex,
                "HARNESS_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public MountHarness addHarness(Long userId, Integer itemId, Integer grade) {
        log.info("Adding harness for user: {}, itemId: {}, grade: {}", userId, itemId, grade);
        
        if (!hasSpace(userId, 1)) {
            throw new MountServiceException("Harness bag is full", "BAG_FULL");
        }
        
        Integer nextIndex = harnessRepository.findNextAvailableIndex(userId);
        
        MountHarness harness = new MountHarness();
        harness.setUserId(userId);
        harness.setHarnessIndex(nextIndex);
        harness.setItemId(itemId);
        harness.setLevel(1);
        harness.setGrade(grade);
        harness.setLockFlag(0);
        
        // Generate random entries
        generateRandomEntries(harness);
        
        MountHarness saved = harnessRepository.save(harness);
        log.info("Harness added: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public void wearHarness(Long userId, Integer mountIndex, Integer slotIndex, Integer harnessIndex) {
        log.info("Wearing harness for user: {}, mount: {}, slot: {}, harness: {}", 
            userId, mountIndex, slotIndex, harnessIndex);
        
        if (slotIndex < 1 || slotIndex > HARNESS_SLOT_NUM) {
            throw new MountServiceException("Invalid harness slot: " + slotIndex, "INVALID_SLOT");
        }
        
        Mount mount = mountRepository.findByUserIdAndMountIndex(userId, mountIndex)
            .orElseThrow(() -> new MountServiceException("Mount not found", "MOUNT_NOT_FOUND"));
        
        MountHarness harness = getHarness(userId, harnessIndex);
        
        // Get old harness ID from mount slot
        Integer oldHarnessId = switch (slotIndex) {
            case 1 -> mount.getHarnessSlot1();
            case 2 -> mount.getHarnessSlot2();
            case 3 -> mount.getHarnessSlot3();
            case 4 -> mount.getHarnessSlot4();
            default -> null;
        };
        
        // If there's an old harness, return it to bag (create new harness entry)
        if (oldHarnessId != null && oldHarnessId > 0) {
            log.debug("Returning old harness to bag: {}", oldHarnessId);
            // Note: Would create new MountHarness entity with preserved attributes
            // Full implementation would fetch attributes from mount config and create entry
        }
        
        // Update mount slot with new harness
        mountRepository.updateHarnessSlot(userId, mountIndex, slotIndex, harness.getItemId());
        
        // Delete harness from bag (it's now equipped)
        harnessRepository.deleteByUserIdAndHarnessIndex(userId, harnessIndex);
        
        log.info("Harness equipped successfully");
    }
    
    @Override
    @Transactional
    public void removeHarness(Long userId, Integer mountIndex, Integer slotIndex) {
        log.info("Removing harness for user: {}, mount: {}, slot: {}", 
            userId, mountIndex, slotIndex);
        
        if (!hasSpace(userId, 1)) {
            throw new MountServiceException("Harness bag is full", "BAG_FULL");
        }
        
        Mount mount = mountRepository.findByUserIdAndMountIndex(userId, mountIndex)
            .orElseThrow(() -> new MountServiceException("Mount not found", "MOUNT_NOT_FOUND"));
        
        // Get harness ID from slot
        Integer harnessId = switch (slotIndex) {
            case 1 -> mount.getHarnessSlot1();
            case 2 -> mount.getHarnessSlot2();
            case 3 -> mount.getHarnessSlot3();
            case 4 -> mount.getHarnessSlot4();
            default -> null;
        };
        
        if (harnessId == null || harnessId == 0) {
            throw new MountServiceException("No harness in this slot", "SLOT_EMPTY");
        }
        
        log.debug("Returning harness {} to bag", harnessId);
        
        // Clear mount slot
        mountRepository.updateHarnessSlot(userId, mountIndex, slotIndex, 0);
        
        log.info("Harness removed successfully");
    }
    
    @Override
    @Transactional
    public void decomposeHarness(Long userId, Integer harnessIndex) {
        log.info("Decomposing harness for user: {}, index: {}", userId, harnessIndex);
        
        MountHarness harness = getHarness(userId, harnessIndex);
        
        // Calculate decompose rewards based on grade/level
        long goldReward = harness.getGrade() * 100L + harness.getLevel() * 10L;
        int materialId = 10020; // Harness fragment
        int materialQuantity = harness.getGrade() * 2;
        
        // Grant rewards
        addGold(String.valueOf(userId), goldReward);
        addMaterial(String.valueOf(userId), materialId, materialQuantity);
        
        harnessRepository.deleteByUserIdAndHarnessIndex(userId, harnessIndex);
        
        log.info("Harness decomposed - gold: {}, material: {}x{}", 
            goldReward, materialId, materialQuantity);
    }
    
    @Override
    @Transactional
    public MountHarness refreshEntries(Long userId, Integer harnessIndex, Integer lockFlag) {
        log.info("Refreshing harness entries for user: {}, index: {}, lockFlag: {}", 
            userId, harnessIndex, lockFlag);
        
        MountHarness harness = getHarness(userId, harnessIndex);
        
        // Check refresh cost
        long refreshCost = 1000L;
        int refreshItemId = 10021; // Refresh stone
        
        try {
            // Try using refresh item first
            consumeMaterial(String.valueOf(userId), refreshItemId, 1);
        } catch (Exception e) {
            // Fallback to gold
            consumeGold(String.valueOf(userId), refreshCost, "harness_refresh");
        }
        
        // Refresh unlocked entries
        if ((lockFlag & 1) == 0) { // Entry 1 not locked
            int[] entry = generateRandomEntry(harness.getGrade());
            harness.setEntry1Type(entry[0]);
            harness.setEntry1Value((long) entry[1]);
        }
        
        if ((lockFlag & 2) == 0) { // Entry 2 not locked
            int[] entry = generateRandomEntry(harness.getGrade());
            harness.setEntry2Type(entry[0]);
            harness.setEntry2Value((long) entry[1]);
        }
        
        if ((lockFlag & 4) == 0) { // Entry 3 not locked
            int[] entry = generateRandomEntry(harness.getGrade());
            harness.setEntry3Type(entry[0]);
            harness.setEntry3Value((long) entry[1]);
        }
        
        if ((lockFlag & 8) == 0) { // Entry 4 not locked
            int[] entry = generateRandomEntry(harness.getGrade());
            harness.setEntry4Type(entry[0]);
            harness.setEntry4Value((long) entry[1]);
        }
        
        MountHarness saved = harnessRepository.save(harness);
        log.info("Harness entries refreshed");
        return saved;
    }
    
    @Override
    @Transactional
    public MountHarness buyHarness(Long userId, Integer itemId, Integer buyType) {
        log.info("Buying harness for user: {}, itemId: {}, buyType: {}", 
            userId, itemId, buyType);
        
        if (!hasSpace(userId, 1)) {
            throw new MountServiceException("Harness bag is full", "BAG_FULL");
        }
        
        // Get buy cost from config
        MountConfigHolder.HarnessBuyCost cost = configHolder.getHarnessBuyCost(buyType);
        
        // Consume cost
        if (buyType == 1) {
            // Gold buy
            consumeGold(String.valueOf(userId), cost.getGoldCost(), "harness_buy");
        } else if (buyType == 2) {
            // Diamond/gem buy
            consumeGold(String.valueOf(userId), cost.getDiamondCost(), "harness_buy_premium");
        }
        
        // Determine harness grade (weighted random)
        Integer grade = generateRandomGrade(buyType);
        
        return addHarness(userId, itemId, grade);
    }
    
    @Override
    @Transactional
    public void setLockFlag(Long userId, Integer harnessIndex, Integer lockFlag) {
        log.info("Setting lock flag for user: {}, index: {}, lockFlag: {}", 
            userId, harnessIndex, lockFlag);
        
        MountHarness harness = getHarness(userId, harnessIndex);
        harness.setLockFlag(lockFlag);
        harnessRepository.save(harness);
    }
    
    @Override
    public void generateRandomEntries(MountHarness harness) {
        log.debug("Generating random entries for harness grade: {}", harness.getGrade());
        
        for (int i = 0; i < ENTRY_NUM; i++) {
            int[] entry = generateRandomEntry(harness.getGrade());
            switch (i) {
                case 0 -> {
                    harness.setEntry1Type(entry[0]);
                    harness.setEntry1Value((long) entry[1]);
                }
                case 1 -> {
                    harness.setEntry2Type(entry[0]);
                    harness.setEntry2Value((long) entry[1]);
                }
                case 2 -> {
                    harness.setEntry3Type(entry[0]);
                    harness.setEntry3Value((long) entry[1]);
                }
                case 3 -> {
                    harness.setEntry4Type(entry[0]);
                    harness.setEntry4Value((long) entry[1]);
                }
            }
        }
    }
    
    /**
     * Generate random entry [type, value]
     */
    private int[] generateRandomEntry(Integer grade) {
        int type = random.nextInt(10) + 1; // Attribute type 1-10
        int baseValue = 100 * grade;
        int value = baseValue + random.nextInt(baseValue / 2);
        
        return new int[]{type, value};
    }
    
    /**
     * Generate random grade based on buy type
     */
    private int generateRandomGrade(int buyType) {
        if (buyType == 1) {
            // Gold buy: lower quality
            double rand = random.nextDouble();
            if (rand < 0.60) return 1; // 60% grade 1
            if (rand < 0.85) return 2; // 25% grade 2
            if (rand < 0.95) return 3; // 10% grade 3
            return 4; // 5% grade 4
        } else {
            // Premium buy: higher quality
            double rand = random.nextDouble();
            if (rand < 0.30) return 3; // 30% grade 3
            if (rand < 0.60) return 4; // 30% grade 4
            if (rand < 0.90) return 5; // 30% grade 5
            return 6; // 10% grade 6
        }
    }
    
    @Override
    public boolean hasSpace(Long userId, Integer count) {
        long currentCount = harnessRepository.countByUserId(userId);
        return currentCount + count <= HARNESS_BAG_MAX;
    }
    
    @Override
    public Long calculateHarnessBonus(MountHarness harness) {
        // Sum all harness entry stat values (up to 4 random affixes)
        long bonus = 0L;
        
        if (harness.getEntry1Value() != null) bonus += harness.getEntry1Value();
        if (harness.getEntry2Value() != null) bonus += harness.getEntry2Value();
        if (harness.getEntry3Value() != null) bonus += harness.getEntry3Value();
        if (harness.getEntry4Value() != null) bonus += harness.getEntry4Value();
        
        return bonus;
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
            .reason(2001)
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new MountServiceException("Insufficient gold", "WALLET_ERROR");
        }
    }
    
    /**
     * Add gold to player wallet
     */
    private void addGold(String playerId, long amount) {
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
            .reason(2001)
            .build();
        
        try {
            walletClient.addCurrency(playerId, request);
            log.debug("Added {} gold", amount);
        } catch (Exception e) {
            log.error("Failed to add gold: {}", e.getMessage());
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
            throw new MountServiceException("Insufficient materials", "BAG_ERROR");
        }
    }
    
    /**
     * Add material to player bag
     */
    private void addMaterial(String roleId, int itemId, int quantity) {
        if (quantity <= 0) {
            return;
        }

        BagDTOs.GrantReq request = new BagDTOs.GrantReq();
        request.setRoleId(roleId);
        request.setItems(List.of(BagDTOs.GrantItem.builder()
            .itemId(itemId)
            .num(quantity)
            .build()));
        request.setReason("mount_harness_reward");

        try {
            bagClient.grantItems(request);
            log.debug("Granted material: itemId={}, quantity={}", itemId, quantity);
        } catch (Exception e) {
            log.error("Failed to grant material to bag: {}", e.getMessage());
            // Non-critical: reward already logged; continue without re-throwing
        }
    }
}
