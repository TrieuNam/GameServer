package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.model.entity.PetTSGem;
import com.SouthMillion.pet_service.service.PetGemService;
import com.SouthMillion.pet_service.service.PetTSGemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pet Gem Controller
 * Handles normal and special gem operations
 */
@RestController
@RequestMapping("/api/pet/gem")
@RequiredArgsConstructor
@Slf4j
public class PetGemController {

    private final PetGemService gemService;
    private final PetTSGemService tsGemService;

    // ==================== Normal Gems ====================

    @PostMapping("/{userId}/inlay")
    public ResponseEntity<Void> inlayGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex,
            @RequestParam Integer gemItemId) {
        log.info("POST /api/pet/gem/{}/inlay - petIndex={}, slot={}, gemId={}", 
            userId, petIndex, slotIndex, gemItemId);
        gemService.inlayGem(userId, petIndex, slotIndex, gemItemId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/levelup/bag")
    public ResponseEntity<Void> gemLevelUpBag(
            @PathVariable String userId,
            @RequestParam Integer itemId,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/gem/{}/levelup/bag - itemId={}, materials={}", 
            userId, itemId, materials);
        gemService.gemLevelUpBag(userId, itemId, materials);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/levelup/pet")
    public ResponseEntity<Void> gemLevelUpPet(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/gem/{}/levelup/pet - petIndex={}, slot={}, materials={}", 
            userId, petIndex, slotIndex, materials);
        gemService.gemLevelUpPet(userId, petIndex, slotIndex, materials);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/onekey")
    public ResponseEntity<Void> oneKeyGemLevelUp(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex) {
        log.info("POST /api/pet/gem/{}/onekey - petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        gemService.oneKeyGemLevelUp(userId, petIndex, slotIndex);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/dismount")
    public ResponseEntity<Void> dismountGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex) {
        log.info("DELETE /api/pet/gem/{}/dismount - petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        gemService.dismountGem(userId, petIndex, slotIndex);
        return ResponseEntity.ok().build();
    }

    // ==================== Special Gems (TS Gem) ====================

    @PostMapping("/{userId}/tsgem/add")
    public ResponseEntity<PetTSGem> addTSGem(
            @PathVariable String userId,
            @RequestParam Integer level) {
        log.info("POST /api/pet/gem/{}/tsgem/add - level={}", userId, level);
        PetTSGem gem = tsGemService.addTSGem(userId, level);
        return ResponseEntity.ok(gem);
    }

    @PostMapping("/{userId}/tsgem/inlay")
    public ResponseEntity<Void> inlayTSGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex,
            @RequestParam Integer gemIndex) {
        log.info("POST /api/pet/gem/{}/tsgem/inlay - petIndex={}, slot={}, gemIndex={}", 
            userId, petIndex, slotIndex, gemIndex);
        tsGemService.inlayTSGem(userId, petIndex, slotIndex, gemIndex);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/tsgem/levelup")
    public ResponseEntity<Void> tsGemLevelUp(
            @PathVariable String userId,
            @RequestParam Integer gemIndex,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/gem/{}/tsgem/levelup - gemIndex={}, materials={}", 
            userId, gemIndex, materials);
        tsGemService.tsGemLevelUp(userId, gemIndex, materials);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/tsgem/onekey")
    public ResponseEntity<Void> oneKeyTSGemLevelUp(
            @PathVariable String userId,
            @RequestParam Integer gemIndex) {
        log.info("POST /api/pet/gem/{}/tsgem/onekey - gemIndex={}", userId, gemIndex);
        tsGemService.oneKeyTSGemLevelUp(userId, gemIndex);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/tsgem/refresh")
    public ResponseEntity<Void> tsGemRefresh(
            @PathVariable String userId,
            @RequestParam Integer gemIndex,
            @RequestParam(defaultValue = "0") Integer lockFlag) {
        log.info("POST /api/pet/gem/{}/tsgem/refresh - gemIndex={}, lockFlag={}", 
            userId, gemIndex, lockFlag);
        tsGemService.tsGemRefresh(userId, gemIndex, lockFlag);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/tsgem/addattr")
    public ResponseEntity<Void> addTSGemAttr(
            @PathVariable String userId,
            @RequestParam Integer gemIndex) {
        log.info("POST /api/pet/gem/{}/tsgem/addattr - gemIndex={}", userId, gemIndex);
        tsGemService.addTSGemAttr(userId, gemIndex);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/tsgem/dismount")
    public ResponseEntity<Void> dismountTSGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex) {
        log.info("DELETE /api/pet/gem/{}/tsgem/dismount - petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        tsGemService.dismountTSGem(userId, petIndex, slotIndex);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/tsgem/{gemIndex}")
    public ResponseEntity<PetTSGem> getTSGem(
            @PathVariable String userId,
            @PathVariable Integer gemIndex) {
        log.debug("GET /api/pet/gem/{}/tsgem/{}", userId, gemIndex);
        PetTSGem gem = tsGemService.getTSGem(userId, gemIndex);
        return ResponseEntity.ok(gem);
    }

    @GetMapping("/{userId}/tsgem/{gemIndex}/canrefresh")
    public ResponseEntity<Boolean> canRefreshTSGem(
            @PathVariable String userId,
            @PathVariable Integer gemIndex) {
        boolean canRefresh = tsGemService.canRefresh(userId, gemIndex);
        return ResponseEntity.ok(canRefresh);
    }
}
