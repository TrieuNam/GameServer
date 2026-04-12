package com.SouthMillion.rune_service.service.impl;

import com.SouthMillion.rune_service.client.BagClient;
import com.SouthMillion.rune_service.client.RoleServiceClient;
import com.SouthMillion.rune_service.client.WalletClient;
import com.SouthMillion.rune_service.config.InscriptionTowerConfigProvider;
import com.SouthMillion.rune_service.config.InscriptionConfigProvider;
import com.SouthMillion.rune_service.exception.RuneServiceException;
import com.SouthMillion.rune_service.model.entity.Rune;
import com.SouthMillion.rune_service.model.entity.RuneTowerState;
import com.SouthMillion.rune_service.repository.RuneRepository;
import com.SouthMillion.rune_service.repository.RuneTowerStateRepository;
import com.SouthMillion.rune_service.service.RuneService;
import com.SouthMillion.rune_service.util.RunePowerCalculator;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuneServiceImpl implements RuneService {

    private final RuneRepository runeRepository;
    private final RuneTowerStateRepository towerStateRepository;
    private final WalletClient walletClient;
    private final BagClient bagClient;
    private final RunePowerCalculator powerCalculator;
    private final RoleServiceClient roleServiceClient;
    private final InscriptionTowerConfigProvider inscriptionTowerConfigProvider;
    private final InscriptionConfigProvider inscriptionConfigProvider;
    private final Random random = new Random();

    /** Item ID for the inscription upgrade currency (水晶 70100) */
    private static final int UP_ITEM_ID = 70100;
    
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
    @Transactional
    public int offRune(Long userId, Integer equipSlot) {
        log.info("OffRune by slot for user: {}, slot: {}", userId, equipSlot);
        Optional<Rune> opt = runeRepository.findByUserIdAndEquipSlot(userId, equipSlot);
        if (opt.isEmpty()) {
            throw new RuneServiceException("No rune equipped in slot " + equipSlot, "SLOT_EMPTY");
        }
        Rune rune = opt.get();
        int knapsackIndex = rune.getRuneIndex();
        Long powerDelta = -powerCalculator.calculateTotalRunePower(rune);
        rune.setIsEquipped(false);
        rune.setEquipSlot(null);
        runeRepository.save(rune);
        updateRoleCapability(String.valueOf(userId), powerDelta, "rune_unequip");
        return knapsackIndex;
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

    // ── op=5 UPRUNE ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public Rune upRune(Long userId, Integer runeIndex) {
        log.info("UpRune user={} runeIndex={}", userId, runeIndex);
        Rune rune = getRune(userId, runeIndex);
        JsonNode config = inscriptionConfigProvider.getConfig();
        if (config == null) {
            throw new RuneServiceException("Rune config unavailable", "RUNE_CONFIG_MISSING");
        }
        JsonNode otherArr = config.path("other");
        JsonNode otherNode = otherArr.isArray() && !otherArr.isEmpty() ? otherArr.get(0) : null;
        int maxLevel = readInt(otherNode, "max_level", 30);
        if (rune.getLevel() >= maxLevel) {
            throw new RuneServiceException("Rune already at max level " + maxLevel, "MAX_RUNE_LEVEL");
        }
        // Find upgrade entry matching quality (color) and current level
        int cost = findUpgradeCost(config, rune.getQuality(), rune.getLevel());
        if (cost <= 0) {
            throw new RuneServiceException("No upgrade config for quality=" + rune.getQuality()
                    + " level=" + rune.getLevel(), "RUNE_UPGRADE_CONFIG_MISSING");
        }
        consumeMaterial(String.valueOf(userId), UP_ITEM_ID, cost);
        rune.setLevel(rune.getLevel() + 1);
        long attrIncrement = rune.getQuality() * 10L;
        rune.setMainAttrValue(rune.getMainAttrValue() + attrIncrement);
        return runeRepository.save(rune);
    }

    private int findUpgradeCost(JsonNode config, int quality, int currentLevel) {
        JsonNode upgradeArr = config.path("upgrade");
        if (!upgradeArr.isArray()) return 0;
        for (JsonNode entry : upgradeArr) {
            if (readInt(entry, "color", -1) == quality && readInt(entry, "level", -1) == currentLevel) {
                return readInt(entry, "exp", 0);
            }
        }
        return 0;
    }

    // ── op=6 DECOMPOSE ────────────────────────────────────────────────────────
    @Override
    @Transactional
    public Map<String, Object> decomposeRunes(Long userId,
                                               List<Integer> knapsackIndices,
                                               List<Integer> crystalItemIds) {
        log.info("DecomposeRunes user={} runes={} crystals={}", userId, knapsackIndices, crystalItemIds);
        JsonNode config = inscriptionConfigProvider.getConfig();
        int totalRefund = 0;

        // Decompose runes → refund ins_back_num of UP_ITEM_ID
        if (knapsackIndices != null) {
            for (int idx : knapsackIndices) {
                Rune rune = getRune(userId, idx);
                if (rune.getIsEquipped()) {
                    throw new RuneServiceException("Cannot decompose equipped rune index=" + idx, "RUNE_EQUIPPED");
                }
                if (config != null) {
                    totalRefund += findDecomposeRefund(config, rune.getQuality(), rune.getLevel());
                }
                runeRepository.delete(rune);
            }
        }

        // Decompose exp-crystals → exchange back to UP_ITEM_ID
        // Crystal exchange: consume from bag then calculate refund amount.
        // Crystal IDs 70101-70104 each have an exp value in config.other.
        // We treat that exp value as the refund in UP_ITEM_ID (70100).
        if (crystalItemIds != null && config != null) {
            JsonNode otherNode = config.path("other");
            JsonNode other = otherNode.isArray() && !otherNode.isEmpty() ? otherNode.get(0) : null;
            for (int itemId : crystalItemIds) {
                int exp = findCrystalExp(other, itemId);
                if (exp > 0) {
                    // Consume 1 of this crystal from bag
                    consumeMaterial(String.valueOf(userId), itemId, 1);
                    totalRefund += exp;
                }
            }
        }

        // Give refund items back to bag
        if (totalRefund > 0) {
            giveItem(String.valueOf(userId), UP_ITEM_ID, totalRefund);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("refundItemId", UP_ITEM_ID);
        result.put("refundAmount", totalRefund);
        return result;
    }

    private int findDecomposeRefund(JsonNode config, int quality, int level) {
        JsonNode insBack = config.path("ins_back");
        if (!insBack.isArray()) return 0;
        for (JsonNode entry : insBack) {
            if (readInt(entry, "color", -1) == quality && readInt(entry, "level", -1) == level) {
                return readInt(entry, "ins_back_num", 0);
            }
        }
        // Fallback: lower level match
        int best = 0;
        for (JsonNode entry : insBack) {
            if (readInt(entry, "color", -1) == quality && readInt(entry, "level", 0) <= level) {
                best = readInt(entry, "ins_back_num", 0);
            }
        }
        return best;
    }

    private int findCrystalExp(JsonNode other, int itemId) {
        if (other == null) return 0;
        for (int i = 0; i <= 3; i++) {
            if (readInt(other, "exp_id_" + i, -1) == itemId) {
                return readInt(other, "exp_" + i, 0);
            }
        }
        return 0;
    }

    // ── Tower state helpers ───────────────────────────────────────────────────
    @Override
    public RuneTowerState getTowerState(Long userId) {
        return towerStateRepository.findById(userId)
                .orElseGet(() -> towerStateRepository.save(
                        RuneTowerState.builder().roleId(userId).build()));
    }

    @Override
    @Transactional
    public RuneTowerState fetchDayReward(Long userId) {
        RuneTowerState state = getTowerState(userId);
        if (state.getDailyReward() == 1) {
            throw new RuneServiceException("Daily reward already claimed", "DAILY_REWARD_CLAIMED");
        }
        JsonNode config = inscriptionTowerConfigProvider.getConfig();
        if (config != null) {
            JsonNode dayRewards = config.path("day_reward");
            if (dayRewards.isArray()) {
                // Find highest eligible tier
                JsonNode eligible = null;
                for (JsonNode entry : dayRewards) {
                    int threshold = readInt(entry, "level_clear", Integer.MAX_VALUE);
                    if (state.getTowerLevel() >= threshold) {
                        eligible = entry;
                    }
                }
                if (eligible != null) {
                    giveItemsFromArray(String.valueOf(userId), eligible.path("reward"));
                }
            }
        }
        state.setDailyReward(1);
        return towerStateRepository.save(state);
    }

    @Override
    @Transactional
    public RuneTowerState passReward(Long userId) {
        RuneTowerState state = getTowerState(userId);
        JsonNode config = inscriptionTowerConfigProvider.getConfig();
        if (config == null) {
            throw new RuneServiceException("Config unavailable", "CONFIG_MISSING");
        }
        JsonNode dayRewards = config.path("day_reward");
        if (!dayRewards.isArray()) {
            throw new RuneServiceException("day_reward config missing", "CONFIG_MISSING");
        }
        int idx = state.getPassRewardIndex();
        if (idx >= dayRewards.size()) {
            throw new RuneServiceException("All pass rewards already claimed", "PASS_REWARD_DONE");
        }
        JsonNode entry = dayRewards.get(idx);
        int threshold = readInt(entry, "level_clear", Integer.MAX_VALUE);
        if (state.getTowerLevel() < threshold) {
            throw new RuneServiceException("Tower level too low for this reward (need "
                    + threshold + ")", "TOWER_LEVEL_TOO_LOW");
        }
        giveItemsFromArray(String.valueOf(userId), entry.path("reward_chap"));
        state.setPassRewardIndex(idx + 1);
        return towerStateRepository.save(state);
    }

    @Override
    @Transactional
    public RuneTowerState winTowerLevel(Long userId) {
        RuneTowerState state = getTowerState(userId);
        JsonNode config = inscriptionTowerConfigProvider.getConfig();
        if (config != null) {
            JsonNode clearance = config.path("clearance");
            if (clearance.isArray()) {
                // Give reward for the level just beaten (1-indexed)
                int beaten = state.getTowerLevel() + 1;
                for (JsonNode entry : clearance) {
                    if (readInt(entry, "level", -1) == beaten) {
                        giveItemsFromArray(String.valueOf(userId), entry.path("win"));
                        // Bonus turntable spins on every 10th level
                        if (beaten % 10 == 0) {
                            state.setTurntableNum(state.getTurntableNum() + 1);
                        }
                        break;
                    }
                }
            }
        }
        state.setTowerLevel(state.getTowerLevel() + 1);
        return towerStateRepository.save(state);
    }

    // ── op=2 TURNTABLE ────────────────────────────────────────────────────────
    @Override
    @Transactional
    public Map<String, Object> turntable(Long userId, int count) {
        log.info("Turntable user={} count={}", userId, count);
        RuneTowerState state = getTowerState(userId);
        JsonNode config = inscriptionTowerConfigProvider.getConfig();
        if (config == null) {
            throw new RuneServiceException("Config unavailable", "CONFIG_MISSING");
        }

        // Determine spin cost per spin from price config
        JsonNode priceArr = config.path("price");
        JsonNode price = priceArr.isArray() && !priceArr.isEmpty() ? priceArr.get(0) : null;
        JsonNode costArr = price != null ? price.path("cost") : null;

        // Consume spin cost for all requested spins
        if (costArr != null && costArr.isArray()) {
            for (JsonNode costEntry : costArr) {
                int itemId = readInt(costEntry, "item_id", 0);
                int num = readInt(costEntry, "num", 0);
                if (itemId > 0 && num > 0) {
                    consumeMaterial(String.valueOf(userId), itemId, num * count);
                }
            }
        }

        JsonNode turntableArr = config.path("turntable");
        List<Map<String, Object>> spinResults = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Map<String, Object> spinResult = performOneSpin(userId, state, turntableArr);
            spinResults.add(spinResult);
        }

        towerStateRepository.save(state);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("state", stateToMap(state));
        result.put("spinResults", spinResults);
        return result;
    }

    private Map<String, Object> performOneSpin(Long userId, RuneTowerState state, JsonNode turntableArr) {
        int currentRound = state.getTurntableRound() + 1; // config is 1-indexed
        int currentFlag  = state.getTurntableFlag();

        // Count total slots for this round
        int totalSlots = 0;
        for (JsonNode entry : turntableArr) {
            if (readInt(entry, "cur_round", -1) == currentRound) totalSlots++;
        }
        if (totalSlots == 0) totalSlots = 8; // default

        // Find uncollected slots (bits not yet set)
        List<Integer> available = new ArrayList<>();
        for (int slot = 1; slot <= totalSlots; slot++) {
            if ((currentFlag & (1 << (slot - 1))) == 0) {
                available.add(slot);
            }
        }
        if (available.isEmpty()) {
            // All collected — reset
            state.setTurntableFlag(0);
            state.setTurntableRound(state.getTurntableRound() + 1);
            currentRound = state.getTurntableRound() + 1;
            currentFlag  = 0;
            available.clear();
            for (int slot = 1; slot <= totalSlots; slot++) available.add(slot);
        }

        int pickedSlot = available.get(random.nextInt(available.size()));
        int newFlag = currentFlag | (1 << (pickedSlot - 1));
        state.setTurntableFlag(newFlag);

        // Check if round complete
        int fullMask = (1 << totalSlots) - 1;
        if ((newFlag & fullMask) == fullMask) {
            state.setTurntableFlag(0);
            state.setTurntableRound(state.getTurntableRound() + 1);
        }

        // Find and give prize
        List<Map<String, Object>> rewards = new ArrayList<>();
        for (JsonNode entry : turntableArr) {
            if (readInt(entry, "cur_round", -1) == currentRound
                    && readInt(entry, "cur_index", -1) == pickedSlot) {
                JsonNode wins = entry.path("win");
                if (wins.isArray()) {
                    for (JsonNode win : wins) {
                        int itemId = readInt(win, "item_id", 0);
                        int num    = readInt(win, "num", 0);
                        if (itemId > 0 && num > 0) {
                            giveItem(String.valueOf(userId), itemId, num);
                            Map<String, Object> rw = new HashMap<>();
                            rw.put("itemId", itemId);
                            rw.put("num", num);
                            rewards.add(rw);
                        }
                    }
                }
                break;
            }
        }

        Map<String, Object> spinResult = new HashMap<>();
        spinResult.put("slotBit", pickedSlot);
        spinResult.put("rewards", rewards);
        return spinResult;
    }

    private Map<String, Object> stateToMap(RuneTowerState state) {
        Map<String, Object> m = new HashMap<>();
        m.put("towerLevel", state.getTowerLevel());
        m.put("turntableNum", state.getTurntableNum());
        m.put("turntableFlag", state.getTurntableFlag());
        m.put("turntableRound", state.getTurntableRound());
        m.put("dailyReward", state.getDailyReward());
        m.put("passRewardIndex", state.getPassRewardIndex());
        return m;
    }

    // ── Item helpers ──────────────────────────────────────────────────────────
    private void giveItem(String roleId, int itemId, int num) {
        if (num <= 0) return;
        try {
            BagDTOs.GrantReq req = BagDTOs.GrantReq.builder()
                    .roleId(roleId)
                    .items(List.of(BagDTOs.GrantItem.builder()
                            .itemId(itemId)
                            .num(num)
                            .build()))
                    .source("rune-service")
                    .build();
            bagClient.grantItems(req);
        } catch (Exception e) {
            log.error("Failed to give item itemId={} num={} to roleId={}: {}", itemId, num, roleId, e.getMessage());
        }
    }

    private void giveItemsFromArray(String roleId, JsonNode winArr) {
        if (winArr == null || !winArr.isArray()) return;
        for (JsonNode entry : winArr) {
            int itemId = readInt(entry, "item_id", 0);
            int num    = readInt(entry, "num", 0);
            if (itemId > 0 && num > 0) giveItem(roleId, itemId, num);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> drawBox(Long userId, Integer type) {
        int drawType = normalizeDrawType(type);
        JsonNode config = inscriptionTowerConfigProvider.getConfig();
        if (config == null) {
            throw new RuneServiceException("Inscription tower config unavailable", "RUNE_BOX_CONFIG_MISSING");
        }

        JsonNode priceList = config.path("price");
        JsonNode price = priceList.isArray() && !priceList.isEmpty() ? priceList.get(0) : null;
        int rewardNum = readInt(price, "reward_num", 3);
        long cost = switch (drawType) {
            case 1 -> readLong(price, "price1", 0L);
            case 2 -> readLong(price, "price2", 0L);
            default -> 0L;
        };
        if (cost > 0L) {
            consumeDiamonds(String.valueOf(userId), cost, "rune_box_draw");
        }

        List<JsonNode> pool = buildRuneBoxPool(config.path("box"), drawType);
        if (pool.isEmpty()) {
            throw new RuneServiceException("No rune box rewards configured", "RUNE_BOX_REWARD_MISSING");
        }

        List<Map<String, Object>> rewards = new ArrayList<>();
        for (int i = 0; i < Math.max(1, rewardNum); i++) {
            JsonNode selected = pickWeighted(pool);
            if (selected == null) {
                continue;
            }
            JsonNode wins = selected.path("win");
            if (!wins.isArray()) {
                continue;
            }
            for (JsonNode win : wins) {
                int runeId = readInt(win, "item_id", 0);
                int num = readInt(win, "num", 0);
                if (runeId <= 0 || num <= 0) {
                    continue;
                }
                for (int count = 0; count < num; count++) {
                    createRune(userId, runeId, 1);
                }
                Map<String, Object> reward = new HashMap<>();
                reward.put("itemId", runeId);
                reward.put("num", num);
                rewards.add(reward);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("drawType", drawType);
        result.put("rewards", rewards);
        return result;
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

    private void consumeDiamonds(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }

        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
                .itemId(3L)
                .amount(amount)
                .build());

        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
                .roleId(playerId)
                .changes(changes)
                .idemKey(UUID.randomUUID().toString())
                .reason(4001)
                .build();

        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} diamonds for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume diamonds: {}", e.getMessage());
            throw new RuneServiceException("Insufficient diamonds: " + e.getMessage(), "WALLET_ERROR");
        }
    }
    
    /**
     * Consume material from player bag
     */
    private void consumeMaterial(String roleId, int itemId, int quantity) {
        if (quantity <= 0) {
            return;
        }

        BagDTOs.UseItemReq request = BagDTOs.UseItemReq.builder()
                .itemId(itemId)
                .num(quantity)
                .build();

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

    private int normalizeDrawType(Integer type) {
        if (type == null || type < 0 || type > 2) {
            return 0;
        }
        return type;
    }

    private List<JsonNode> buildRuneBoxPool(JsonNode boxNode, int drawType) {
        List<JsonNode> pool = new ArrayList<>();
        if (boxNode != null && boxNode.isArray()) {
            for (JsonNode node : boxNode) {
                if (readInt(node, "type", -1) != drawType) {
                    continue;
                }
                if (readInt(node, "rate", 0) <= 0) {
                    continue;
                }
                pool.add(node);
            }
        }
        return pool;
    }

    private JsonNode pickWeighted(List<JsonNode> pool) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (JsonNode node : pool) {
            totalWeight += Math.max(0, readInt(node, "rate", 0));
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (JsonNode node : pool) {
            cumulative += Math.max(0, readInt(node, "rate", 0));
            if (roll < cumulative) {
                return node;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private int readInt(JsonNode node, String field, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        if (value.isNumber()) {
            return value.intValue();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private long readLong(JsonNode node, String field, long defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        if (value.isNumber()) {
            return value.longValue();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
