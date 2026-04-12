package com.SouthMillion.rune_service.service;

import com.SouthMillion.rune_service.model.entity.Rune;
import com.SouthMillion.rune_service.model.entity.RuneTowerState;

import java.util.List;
import java.util.Map;

public interface RuneService {

    // ── Knapsack ──────────────────────────────────────────────────────────────
    List<Rune> getAllRunes(Long userId);
    Rune getRune(Long userId, Integer runeIndex);
    Rune createRune(Long userId, Integer runeId, Integer quality);
    void deleteRune(Long userId, Integer runeIndex);

    // ── Legacy upgrade (internal / REST) ─────────────────────────────────────
    Rune levelUpRune(Long userId, Integer runeIndex);
    Rune upgradeRuneQuality(Long userId, Integer runeIndex);
    Rune upgradeRuneStar(Long userId, Integer runeIndex);
    Rune refineRune(Long userId, Integer runeIndex);
    Rune addRuneExp(Long userId, Integer runeIndex, Long exp);

    // ── Equipment ops ─────────────────────────────────────────────────────────
    /** op=3 WEARRUNE: equip rune at knapsackIndex to equipSlot */
    Rune equipRune(Long userId, Integer runeIndex, Integer equipSlot);
    /** Legacy unequip by knapsack index */
    void unequipRune(Long userId, Integer runeIndex);
    void unequipRuneFromSlot(Long userId, Integer equipSlot);
    /** op=4 OFFRUNE: unequip rune by slot, returns the freed knapsack index */
    int offRune(Long userId, Integer equipSlot);
    List<Rune> getEquippedRunes(Long userId);

    // ── Upgrade (op=5 UPRUNE) ─────────────────────────────────────────────────
    /** Upgrades rune by consuming up_item (70100) based on inscription upgrade config */
    Rune upRune(Long userId, Integer runeIndex);

    // ── Decompose (op=6) ──────────────────────────────────────────────────────
    /**
     * Decomposes runes and exp-crystals.
     * @param knapsackIndices rune slots to decompose (may be empty)
     * @param crystalItemIds  exp-crystal item IDs to decompose (may be empty)
     * @return map with refundItemId and refundAmount
     */
    Map<String, Object> decomposeRunes(Long userId,
                                       List<Integer> knapsackIndices,
                                       List<Integer> crystalItemIds);

    // ── Tower state ───────────────────────────────────────────────────────────
    RuneTowerState getTowerState(Long userId);
    /** op=1 FETCHDAYREWARD */
    RuneTowerState fetchDayReward(Long userId);
    /** op=7 PASS_REWARD */
    RuneTowerState passReward(Long userId);
    /** Called after winning INSCRIPTION_TOWER battle */
    RuneTowerState winTowerLevel(Long userId);

    // ── Turntable (op=2) ──────────────────────────────────────────────────────
    /** Spin the turntable `count` times */
    Map<String, Object> turntable(Long userId, int count);

    // ── Box draw (op=8) ───────────────────────────────────────────────────────
    Map<String, Object> drawBox(Long userId, Integer type);

    // ── Attribute helpers ─────────────────────────────────────────────────────
    Rune refreshSubAttributes(Long userId, Integer runeIndex);
    Long calculateRunePower(Rune rune);
    Long calculateTotalRunePower(Long userId);

    // ── Validation ────────────────────────────────────────────────────────────
    boolean canLevelUp(Long userId, Integer runeIndex);
    boolean canUpgradeQuality(Long userId, Integer runeIndex);
    boolean canUpgradeStar(Long userId, Integer runeIndex);
}
