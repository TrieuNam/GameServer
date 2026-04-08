package com.SouthMillion.rune_service.service.impl;

import com.SouthMillion.rune_service.client.BagClient;
import com.SouthMillion.rune_service.client.RoleServiceClient;
import com.SouthMillion.rune_service.client.WalletClient;
import com.SouthMillion.rune_service.exception.RuneServiceException;
import com.SouthMillion.rune_service.model.entity.Rune;
import com.SouthMillion.rune_service.repository.RuneRepository;
import com.SouthMillion.rune_service.service.RuneService;
import com.SouthMillion.rune_service.util.RunePowerCalculator;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RuneServiceImpl implements RuneService {

    private final RuneRepository runeRepository;
    private final WalletClient walletClient;
    private final BagClient bagClient;
    private final RunePowerCalculator powerCalculator;
    private final RoleServiceClient roleServiceClient;
    private final Random random = new Random();
    
    private static final int MAX_RUNE_LEVEL = 100;
    private static final int MAX_RUNE_QUALITY = 5; // Orange
    private static final int MAX_RUNE_STAR = 10;
    private static final int MAX_REFINEMENT_LEVEL = 20;
    
    @Override
    public List<Rune> getAllRunes(Long userId) {
        return runeRepository.findByUserId(userId);
    }
    
    @Override
    public Rune getRune(Long userId, Integer runeIndex) {
        return runeRepository.findByUserIdAndRuneIndex(userId, runeIndex)
            .orElseThrow(() -> new RuneServiceException(
                "Rune not found: userId=" + userId + ", runeIndex=" + runeIndex,
                "RUNE_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public Rune createRune(Long userId, Integer runeId, Integer quality) {
        log.info("Creating rune for user: {}, runeId: {}, quality: {}", userId, runeId, quality);
        
        Integer nextIndex = runeRepository.findMaxRuneIndexByUserId(userId) + 1;
        
        Rune rune = new Rune();
        rune.setUserId(userId);
        rune.setRuneIndex(nextIndex);
        rune.setRuneId(runeId);
        rune.setLevel(1);
        rune.setQuality(quality);
        rune.setStar(1);
        rune.setIsEquipped(false);
        rune.setEquipSlot(null);
        rune.setRefinementLevel(0);
        rune.setExp(0L);
        
        // Generate random main attribute
        int mainAttrType = random.nextInt(10) + 1;
        long mainAttrValue = 100L * quality;
        rune.setMainAttrType(mainAttrType);
        rune.setMainAttrValue(mainAttrValue);
        
        // Generate random sub attributes based on quality
        generateSubAttributes(rune, quality);
        
        Rune saved = runeRepository.save(rune);
        log.info("Rune created: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public void deleteRune(Long userId, Integer runeIndex) {
        log.info("Deleting rune for user: {}, runeIndex: {}", userId, runeIndex);
        
        Rune rune = getRune(userId, runeIndex);
        
        if (rune.getIsEquipped()) {
            throw new RuneServiceException("Cannot delete equipped rune", "RUNE_EQUIPPED");
        }
        
        runeRepository.delete(rune);
        log.info("Rune deleted");
    }
    
    @Override
    @Transactional
    public Rune levelUpRune(Long userId, Integer runeIndex) {
        log.info("Leveling up rune for user: {}, runeIndex: {}", userId, runeIndex);
        
        Rune rune = getRune(userId, runeIndex);
        
        if (rune.getLevel() >= MAX_RUNE_LEVEL) {
            throw new RuneServiceException("Rune at max level", "MAX_RUNE_LEVEL");
        }
        
        // Consume materials: progressive cost based on level
        long goldCost = rune.getLevel() * 200L;
        int materialCost = (rune.getLevel() / 10) + 1; // 1-11 rune stones
        
        consumeGold(userId.toString(), goldCost, "rune_levelup");
        consumeMaterial(userId.toString(), 6001, materialCost); // Rune Stone
        
        rune.setLevel(rune.getLevel() + 1);
        rune.setExp(0L);
        
        // Increase main attribute
        long increment = rune.getQuality() * 10L;
        rune.setMainAttrValue(rune.getMainAttrValue() + increment);
        
        return runeRepository.save(rune);
    }
    
    @Override
    @Transactional
    public Rune upgradeRuneQuality(Long userId, Integer runeIndex) {
        log.info("Upgrading rune quality for user: {}, runeIndex: {}", userId, runeIndex);
        
        Rune rune = getRune(userId, runeIndex);
        
        if (rune.getQuality() >= MAX_RUNE_QUALITY) {
            throw new RuneServiceException("Rune at max quality", "MAX_RUNE_QUALITY");
        }
        
        // Consume materials: progressive cost based on quality
        long goldCost = rune.getQuality() * 5000L;
        int essenceCost = rune.getQuality() * 10; // Quality Essence
        
        consumeGold(userId.toString(), goldCost, "rune_quality");
        consumeMaterial(userId.toString(), 6002, essenceCost); // Quality Essence
        
        rune.setQuality(rune.getQuality() + 1);
        
        // Regenerate sub attributes for new quality
        generateSubAttributes(rune, rune.getQuality());
        
        return runeRepository.save(rune);
    }
    
    @Override
    @Transactional
    public Rune upgradeRuneStar(Long userId, Integer runeIndex) {
        log.info("Upgrading rune star for user: {}, runeIndex: {}", userId, runeIndex);
        
        Rune rune = getRune(userId, runeIndex);
        
        if (rune.getStar() >= MAX_RUNE_STAR) {
            throw new RuneServiceException("Rune at max star", "MAX_RUNE_STAR");
        }
        
        // Consume materials: progressive cost based on star
        long goldCost = (rune.getStar() + 1) * 1000L;
        int starCrystal = (rune.getStar() + 1) * 2; // Star Crystal
        
        consumeGold(userId.toString(), goldCost, "rune_star");
        consumeMaterial(userId.toString(), 6003, starCrystal); // Star Crystal
        
        rune.setStar(rune.getStar() + 1);
        
        return runeRepository.save(rune);
    }
    
    @Override
    @Transactional
    public Rune refineRune(Long userId, Integer runeIndex) {
        log.info("Refining rune for user: {}, runeIndex: {}", userId, runeIndex);
        
        Rune rune = getRune(userId, runeIndex);
        
        if (rune.getRefinementLevel() >= MAX_REFINEMENT_LEVEL) {
            throw new RuneServiceException("Rune at max refinement", "MAX_REFINEMENT_LEVEL");
        }
        
        // Consume materials: progressive cost based on refinement level
        long goldCost = (rune.getRefinementLevel() + 1) * 800L;
        int refineCrystal = (rune.getRefinementLevel() + 1) * 3; // Refine Crystal
        
        consumeGold(userId.toString(), goldCost, "rune_refine");
        consumeMaterial(userId.toString(), 6004, refineCrystal); // Refine Crystal
        
        rune.setRefinementLevel(rune.getRefinementLevel() + 1);
        
        // Boost all attributes
        long boost = rune.getQuality() * 5L;
        rune.setMainAttrValue(rune.getMainAttrValue() + boost);
        if (rune.getSubAttr1Value() != null) {
            rune.setSubAttr1Value(rune.getSubAttr1Value() + boost);
        }
        if (rune.getSubAttr2Value() != null) {
            rune.setSubAttr2Value(rune.getSubAttr2Value() + boost);
        }
        if (rune.getSubAttr3Value() != null) {
            rune.setSubAttr3Value(rune.getSubAttr3Value() + boost);
        }
        
        return runeRepository.save(rune);
    }
    
    @Override
    @Transactional
    public Rune addRuneExp(Long userId, Integer runeIndex, Long exp) {
        log.info("Adding exp to rune for user: {}, runeIndex: {}, exp: {}", 
            userId, runeIndex, exp);
        
        Rune rune = getRune(userId, runeIndex);
        rune.setExp(rune.getExp() + exp);
        
        // Auto level up if enough exp (1000 per level)
        long expNeeded = rune.getLevel() * 1000L;
        while (rune.getExp() >= expNeeded && rune.getLevel() < MAX_RUNE_LEVEL) {
            rune.setExp(rune.getExp() - expNeeded);
            rune.setLevel(rune.getLevel() + 1);
            
            // Increase main attribute
            long increment = rune.getQuality() * 10L;
            rune.setMainAttrValue(rune.getMainAttrValue() + increment);
            
            log.info("Rune auto-leveled up to level {}", rune.getLevel());
            expNeeded = rune.getLevel() * 1000L;
        }
        
        return runeRepository.save(rune);
    }
    
    @Override
    @Transactional
    public Rune equipRune(Long userId, Integer runeIndex, Integer equipSlot) {
        log.info("Equipping rune for user: {}, runeIndex: {}, slot: {}",
            userId, runeIndex, equipSlot);

        Rune rune = getRune(userId, runeIndex);

        if (rune.getIsEquipped()) {
            throw new RuneServiceException("Rune already equipped", "RUNE_ALREADY_EQUIPPED");
        }

        // Unequip any rune in this slot
        runeRepository.unequipRuneFromSlot(userId, equipSlot);

        rune.setIsEquipped(true);
        rune.setEquipSlot(equipSlot);

        Rune saved = runeRepository.save(rune);

        // Calculate rune power and update role capability
        Long runePower = powerCalculator.calculateTotalRunePower(saved);
        log.info("Rune equipped: slot={}, power={}", equipSlot, runePower);

        // Update role capability via role-service
        updateRoleCapability(String.valueOf(userId), runePower, "rune_equip");

        return saved;
    }

    @Override
    @Transactional
    public void unequipRune(Long userId, Integer runeIndex) {
        log.info("Unequipping rune for user: {}, runeIndex: {}", userId, runeIndex);

        Rune rune = getRune(userId, runeIndex);

        if (!rune.getIsEquipped()) {
            throw new RuneServiceException("Rune not equipped", "RUNE_NOT_EQUIPPED");
        }

        // Calculate power delta (negative for unequip)
        Long powerDelta = -powerCalculator.calculateTotalRunePower(rune);

        rune.setIsEquipped(false);
        rune.setEquipSlot(null);

        runeRepository.save(rune);

        // Update role capability (remove rune power)
        updateRoleCapability(String.valueOf(userId), powerDelta, "rune_unequip");

        log.info("Rune unequipped: index={}, power delta={}", runeIndex, powerDelta);
    }
    
    @Override
    @Transactional
    public void unequipRuneFromSlot(Long userId, Integer equipSlot) {
        log.info("Unequipping rune from slot for user: {}, slot: {}", userId, equipSlot);
        runeRepository.unequipRuneFromSlot(userId, equipSlot);
    }
    
    @Override
    public List<Rune> getEquippedRunes(Long userId) {
        return runeRepository.findByUserIdAndIsEquippedTrue(userId);
    }
    
    @Override
    @Transactional
    public Rune refreshSubAttributes(Long userId, Integer runeIndex) {
        log.info("Refreshing sub attributes for user: {}, runeIndex: {}", userId, runeIndex);
        
        Rune rune = getRune(userId, runeIndex);
        
        // Consume materials for refresh
        long goldCost = 5000L;
        int refreshStone = 5; // Refresh Stone
        
        consumeGold(userId.toString(), goldCost, "rune_refresh");
        consumeMaterial(userId.toString(), 6005, refreshStone); // Refresh Stone
        
        // Regenerate sub attributes
        generateSubAttributes(rune, rune.getQuality());
        
        return runeRepository.save(rune);
    }
    
    @Override
    public Long calculateRunePower(Rune rune) {
        // Use RunePowerCalculator for accurate config-based power calculation
        return powerCalculator.calculateTotalRunePower(rune);
    }

    @Override
    public Long calculateTotalRunePower(Long userId) {
        List<Rune> runes = runeRepository.findByUserIdAndIsEquippedTrue(userId);
        return runes.stream()
            .mapToLong(this::calculateRunePower)
            .sum();
    }
    
    @Override
    public boolean canLevelUp(Long userId, Integer runeIndex) {
        try {
            Rune rune = getRune(userId, runeIndex);
            if (rune.getLevel() >= MAX_RUNE_LEVEL) {
                return false;
            }
            // Check if player has enough resources
            // In real implementation, query wallet-service and bag-service
            // For now, assume validation happens during actual upgrade
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean canUpgradeQuality(Long userId, Integer runeIndex) {
        try {
            Rune rune = getRune(userId, runeIndex);
            if (rune.getQuality() >= MAX_RUNE_QUALITY) {
                return false;
            }
            // Check if player has enough resources
            // In real implementation, query wallet-service and bag-service
            // For now, assume validation happens during actual upgrade
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean canUpgradeStar(Long userId, Integer runeIndex) {
        try {
            Rune rune = getRune(userId, runeIndex);
            if (rune.getStar() >= MAX_RUNE_STAR) {
                return false;
            }
            // Check if player has enough resources
            // In real implementation, query wallet-service and bag-service
            // For now, assume validation happens during actual upgrade
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void generateSubAttributes(Rune rune, Integer quality) {
        // Number of sub attributes based on quality
        int subAttrCount = Math.min(quality - 1, 3); // 0-3 sub attributes
        
        if (subAttrCount >= 1) {
            rune.setSubAttr1Type(random.nextInt(10) + 1);
            rune.setSubAttr1Value(50L * quality);
        } else {
            rune.setSubAttr1Type(null);
            rune.setSubAttr1Value(null);
        }
        
        if (subAttrCount >= 2) {
            rune.setSubAttr2Type(random.nextInt(10) + 1);
            rune.setSubAttr2Value(50L * quality);
        } else {
            rune.setSubAttr2Type(null);
            rune.setSubAttr2Value(null);
        }
        
        if (subAttrCount >= 3) {
            rune.setSubAttr3Type(random.nextInt(10) + 1);
            rune.setSubAttr3Value(50L * quality);
        } else {
            rune.setSubAttr3Type(null);
            rune.setSubAttr3Value(null);
        }
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
            .reason(4001) // Rune system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new RuneServiceException("Insufficient gold: " + e.getMessage(), "WALLET_ERROR");
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
            throw new RuneServiceException("Insufficient materials: " + e.getMessage(), "BAG_ERROR");
        }
    }

    /**
     * Update role capability via role-service
     * Called when rune is equipped/unequipped to update player combat power
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
                    new RoleServiceClient.CapabilityUpdateRequest("rune", deltaValue, reason);

            roleServiceClient.updateCapability(roleId, request);
            log.info("Updated role capability: roleId={}, delta={}, reason={}", roleId, deltaValue, reason);
        } catch (Exception e) {
            // Log error but don't fail the rune operation
            // Capability update is not critical for rune equip/unequip success
            log.error("Failed to update role capability: roleId={}, delta={}, error={}",
                    roleId, deltaValue, e.getMessage());
        }
    }
}
