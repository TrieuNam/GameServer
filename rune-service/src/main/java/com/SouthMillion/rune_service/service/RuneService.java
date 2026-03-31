package com.SouthMillion.rune_service.service;

import com.SouthMillion.rune_service.model.entity.Rune;

import java.util.List;

public interface RuneService {
    
    // Basic operations
    List<Rune> getAllRunes(Long userId);
    
    Rune getRune(Long userId, Integer runeIndex);
    
    Rune createRune(Long userId, Integer runeId, Integer quality);
    
    void deleteRune(Long userId, Integer runeIndex);
    
    // Upgrade operations
    Rune levelUpRune(Long userId, Integer runeIndex);
    
    Rune upgradeRuneQuality(Long userId, Integer runeIndex);
    
    Rune upgradeRuneStar(Long userId, Integer runeIndex);
    
    Rune refineRune(Long userId, Integer runeIndex);
    
    Rune addRuneExp(Long userId, Integer runeIndex, Long exp);
    
    // Equipment operations
    Rune equipRune(Long userId, Integer runeIndex, Integer equipSlot);
    
    void unequipRune(Long userId, Integer runeIndex);
    
    void unequipRuneFromSlot(Long userId, Integer equipSlot);
    
    List<Rune> getEquippedRunes(Long userId);
    
    // Attribute operations
    Rune refreshSubAttributes(Long userId, Integer runeIndex);
    
    // Power calculations
    Long calculateRunePower(Rune rune);
    
    Long calculateTotalRunePower(Long userId);
    
    // Validation
    boolean canLevelUp(Long userId, Integer runeIndex);
    
    boolean canUpgradeQuality(Long userId, Integer runeIndex);
    
    boolean canUpgradeStar(Long userId, Integer runeIndex);
}
