package com.SouthMillion.rune_service.controller;

import com.SouthMillion.rune_service.model.entity.Rune;
import com.SouthMillion.rune_service.service.RuneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Rune Controller
 * Rune enhancement system operations
 * 
 * MsgIDs: 1670-1672
 */
@RestController
@RequestMapping("/api/rune")
@RequiredArgsConstructor
@Slf4j
public class RuneController {
    
    private final RuneService runeService;
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<Rune>> getAllRunes(@PathVariable Long userId) {
        log.debug("GET /api/rune/{}", userId);
        List<Rune> runes = runeService.getAllRunes(userId);
        return ResponseEntity.ok(runes);
    }

    @PostMapping("/{userId}/box-draw")
    public ResponseEntity<Map<String, Object>> drawBox(
            @PathVariable Long userId,
            @RequestParam Integer type) {
        log.info("POST /api/rune/{}/box-draw - type={}", userId, type);
        return ResponseEntity.ok(runeService.drawBox(userId, type));
    }
    
    @GetMapping("/{userId}/{runeIndex}")
    public ResponseEntity<Rune> getRune(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.debug("GET /api/rune/{}/{}", userId, runeIndex);
        Rune rune = runeService.getRune(userId, runeIndex);
        return ResponseEntity.ok(rune);
    }
    
    @PostMapping("/{userId}/create")
    public ResponseEntity<Rune> createRune(
            @PathVariable Long userId,
            @RequestParam Integer runeId,
            @RequestParam Integer quality) {
        log.info("POST /api/rune/{}/create - runeId={}, quality={}", userId, runeId, quality);
        Rune rune = runeService.createRune(userId, runeId, quality);
        return ResponseEntity.ok(rune);
    }
    
    @DeleteMapping("/{userId}/{runeIndex}")
    public ResponseEntity<Void> deleteRune(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.info("DELETE /api/rune/{}/{}", userId, runeIndex);
        runeService.deleteRune(userId, runeIndex);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{userId}/{runeIndex}/levelup")
    public ResponseEntity<Rune> levelUpRune(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.info("POST /api/rune/{}/{}/levelup", userId, runeIndex);
        Rune rune = runeService.levelUpRune(userId, runeIndex);
        return ResponseEntity.ok(rune);
    }
    
    @PostMapping("/{userId}/{runeIndex}/quality")
    public ResponseEntity<Rune> upgradeQuality(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.info("POST /api/rune/{}/{}/quality", userId, runeIndex);
        Rune rune = runeService.upgradeRuneQuality(userId, runeIndex);
        return ResponseEntity.ok(rune);
    }
    
    @PostMapping("/{userId}/{runeIndex}/star")
    public ResponseEntity<Rune> upgradeStar(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.info("POST /api/rune/{}/{}/star", userId, runeIndex);
        Rune rune = runeService.upgradeRuneStar(userId, runeIndex);
        return ResponseEntity.ok(rune);
    }
    
    @PostMapping("/{userId}/{runeIndex}/refine")
    public ResponseEntity<Rune> refineRune(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.info("POST /api/rune/{}/{}/refine", userId, runeIndex);
        Rune rune = runeService.refineRune(userId, runeIndex);
        return ResponseEntity.ok(rune);
    }
    
    @PostMapping("/{userId}/{runeIndex}/exp")
    public ResponseEntity<Rune> addExp(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex,
            @RequestParam Long exp) {
        log.info("POST /api/rune/{}/{}/exp - exp={}", userId, runeIndex, exp);
        Rune rune = runeService.addRuneExp(userId, runeIndex, exp);
        return ResponseEntity.ok(rune);
    }
    
    @PostMapping("/{userId}/{runeIndex}/equip")
    public ResponseEntity<Rune> equipRune(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex,
            @RequestParam Integer equipSlot) {
        log.info("POST /api/rune/{}/{}/equip - slot={}", userId, runeIndex, equipSlot);
        Rune rune = runeService.equipRune(userId, runeIndex, equipSlot);
        return ResponseEntity.ok(rune);
    }
    
    @DeleteMapping("/{userId}/{runeIndex}/equip")
    public ResponseEntity<Void> unequipRune(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.info("DELETE /api/rune/{}/{}/equip", userId, runeIndex);
        runeService.unequipRune(userId, runeIndex);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{userId}/equipslot/{equipSlot}")
    public ResponseEntity<Void> unequipFromSlot(
            @PathVariable Long userId,
            @PathVariable Integer equipSlot) {
        log.info("DELETE /api/rune/{}/equipslot/{}", userId, equipSlot);
        runeService.unequipRuneFromSlot(userId, equipSlot);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{userId}/equipped")
    public ResponseEntity<List<Rune>> getEquippedRunes(@PathVariable Long userId) {
        log.debug("GET /api/rune/{}/equipped", userId);
        List<Rune> runes = runeService.getEquippedRunes(userId);
        return ResponseEntity.ok(runes);
    }
    
    @PostMapping("/{userId}/{runeIndex}/refresh")
    public ResponseEntity<Rune> refreshAttributes(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        log.info("POST /api/rune/{}/{}/refresh", userId, runeIndex);
        Rune rune = runeService.refreshSubAttributes(userId, runeIndex);
        return ResponseEntity.ok(rune);
    }
    
    @GetMapping("/{userId}/{runeIndex}/power")
    public ResponseEntity<Long> getRunePower(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        Rune rune = runeService.getRune(userId, runeIndex);
        Long power = runeService.calculateRunePower(rune);
        return ResponseEntity.ok(power);
    }
    
    @GetMapping("/{userId}/power")
    public ResponseEntity<Long> getTotalPower(@PathVariable Long userId) {
        Long power = runeService.calculateTotalRunePower(userId);
        return ResponseEntity.ok(power);
    }
    
    @GetMapping("/{userId}/{runeIndex}/canlevelup")
    public ResponseEntity<Boolean> canLevelUp(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        boolean can = runeService.canLevelUp(userId, runeIndex);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{userId}/{runeIndex}/canupgradequality")
    public ResponseEntity<Boolean> canUpgradeQuality(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        boolean can = runeService.canUpgradeQuality(userId, runeIndex);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{userId}/{runeIndex}/canupgradestar")
    public ResponseEntity<Boolean> canUpgradeStar(
            @PathVariable Long userId,
            @PathVariable Integer runeIndex) {
        boolean can = runeService.canUpgradeStar(userId, runeIndex);
        return ResponseEntity.ok(can);
    }
}
