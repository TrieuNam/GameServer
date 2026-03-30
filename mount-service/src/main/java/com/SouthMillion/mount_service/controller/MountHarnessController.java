package com.SouthMillion.mount_service.controller;

import com.SouthMillion.mount_service.model.entity.MountHarness;
import com.SouthMillion.mount_service.service.MountHarnessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mount Harness Controller
 * Handles mount harness/equipment operations
 */
@RestController
@RequestMapping("/api/mount/harness")
@RequiredArgsConstructor
@Slf4j
public class MountHarnessController {
    
    private final MountHarnessService harnessService;
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<MountHarness>> getAllHarness(@PathVariable Long userId) {
        log.debug("GET /api/mount/harness/{}", userId);
        List<MountHarness> harnessList = harnessService.getAllHarness(userId);
        return ResponseEntity.ok(harnessList);
    }
    
    @GetMapping("/{userId}/{harnessIndex}")
    public ResponseEntity<MountHarness> getHarness(
            @PathVariable Long userId,
            @PathVariable Integer harnessIndex) {
        log.debug("GET /api/mount/harness/{}/{}", userId, harnessIndex);
        MountHarness harness = harnessService.getHarness(userId, harnessIndex);
        return ResponseEntity.ok(harness);
    }
    
    @PostMapping("/{userId}/add")
    public ResponseEntity<MountHarness> addHarness(
            @PathVariable Long userId,
            @RequestParam Integer itemId,
            @RequestParam Integer grade) {
        log.info("POST /api/mount/harness/{}/add - itemId={}, grade={}", 
            userId, itemId, grade);
        MountHarness harness = harnessService.addHarness(userId, itemId, grade);
        return ResponseEntity.ok(harness);
    }
    
    @PostMapping("/{userId}/wear")
    public ResponseEntity<Void> wearHarness(
            @PathVariable Long userId,
            @RequestParam Integer mountIndex,
            @RequestParam Integer slotIndex,
            @RequestParam Integer harnessIndex) {
        log.info("POST /api/mount/harness/{}/wear - mount={}, slot={}, harness={}", 
            userId, mountIndex, slotIndex, harnessIndex);
        harnessService.wearHarness(userId, mountIndex, slotIndex, harnessIndex);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{userId}/wear")
    public ResponseEntity<Void> removeHarness(
            @PathVariable Long userId,
            @RequestParam Integer mountIndex,
            @RequestParam Integer slotIndex) {
        log.info("DELETE /api/mount/harness/{}/wear - mount={}, slot={}", 
            userId, mountIndex, slotIndex);
        harnessService.removeHarness(userId, mountIndex, slotIndex);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{userId}/{harnessIndex}/decompose")
    public ResponseEntity<Void> decomposeHarness(
            @PathVariable Long userId,
            @PathVariable Integer harnessIndex) {
        log.info("DELETE /api/mount/harness/{}/{}/decompose", userId, harnessIndex);
        harnessService.decomposeHarness(userId, harnessIndex);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{userId}/{harnessIndex}/refresh")
    public ResponseEntity<MountHarness> refreshEntries(
            @PathVariable Long userId,
            @PathVariable Integer harnessIndex,
            @RequestParam(defaultValue = "0") Integer lockFlag) {
        log.info("POST /api/mount/harness/{}/{}/refresh - lockFlag={}", 
            userId, harnessIndex, lockFlag);
        MountHarness harness = harnessService.refreshEntries(userId, harnessIndex, lockFlag);
        return ResponseEntity.ok(harness);
    }
    
    @PostMapping("/{userId}/buy")
    public ResponseEntity<MountHarness> buyHarness(
            @PathVariable Long userId,
            @RequestParam Integer itemId,
            @RequestParam Integer buyType) {
        log.info("POST /api/mount/harness/{}/buy - itemId={}, buyType={}", 
            userId, itemId, buyType);
        MountHarness harness = harnessService.buyHarness(userId, itemId, buyType);
        return ResponseEntity.ok(harness);
    }
    
    @PutMapping("/{userId}/{harnessIndex}/lock")
    public ResponseEntity<Void> setLockFlag(
            @PathVariable Long userId,
            @PathVariable Integer harnessIndex,
            @RequestParam Integer lockFlag) {
        log.info("PUT /api/mount/harness/{}/{}/lock - lockFlag={}", 
            userId, harnessIndex, lockFlag);
        harnessService.setLockFlag(userId, harnessIndex, lockFlag);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{userId}/hasspace")
    public ResponseEntity<Boolean> hasSpace(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer count) {
        boolean hasSpace = harnessService.hasSpace(userId, count);
        return ResponseEntity.ok(hasSpace);
    }
    
    @GetMapping("/{userId}/{harnessIndex}/bonus")
    public ResponseEntity<Long> getHarnessBonus(
            @PathVariable Long userId,
            @PathVariable Integer harnessIndex) {
        MountHarness harness = harnessService.getHarness(userId, harnessIndex);
        Long bonus = harnessService.calculateHarnessBonus(harness);
        return ResponseEntity.ok(bonus);
    }
}
