package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.model.entity.PetRemains;
import com.SouthMillion.pet_service.service.PetRemainsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Pet Remains Controller
 * Handles pet relics/remains operations
 */
@RestController
@RequestMapping("/api/pet/remains")
@RequiredArgsConstructor
@Slf4j
public class PetRemainsController {

    private final PetRemainsService remainsService;

    @PostMapping("/{userId}/add")
    public ResponseEntity<PetRemains> addRemains(
            @PathVariable String userId,
            @RequestParam Integer remainsId) {
        log.info("POST /api/pet/remains/{}/add - remainsId={}", userId, remainsId);
        PetRemains remains = remainsService.addRemains(userId, remainsId);
        return ResponseEntity.ok(remains);
    }

    @PostMapping("/{userId}/levelup")
    public ResponseEntity<Void> remainsLevelUp(
            @PathVariable String userId,
            @RequestParam Integer remainsIndex,
            @RequestBody Set<Integer> materials) {
        log.info("POST /api/pet/remains/{}/levelup - remainsIndex={}, materials={}", 
            userId, remainsIndex, materials);
        remainsService.remainsLevelUp(userId, remainsIndex, materials);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/{remainsIndex}")
    public ResponseEntity<PetRemains> getRemains(
            @PathVariable String userId,
            @PathVariable Integer remainsIndex) {
        log.debug("GET /api/pet/remains/{}/{}", userId, remainsIndex);
        PetRemains remains = remainsService.getRemains(userId, remainsIndex);
        return ResponseEntity.ok(remains);
    }

    @GetMapping("/{userId}/bonus")
    public ResponseEntity<Long> getRemainsBonus(@PathVariable String userId) {
        Long bonus = remainsService.calculateRemainsBonus(userId);
        return ResponseEntity.ok(bonus);
    }

    @DeleteMapping("/{userId}/{remainsIndex}")
    public ResponseEntity<Void> deleteRemains(
            @PathVariable String userId,
            @PathVariable Integer remainsIndex) {
        log.info("DELETE /api/pet/remains/{}/{}", userId, remainsIndex);
        remainsService.deleteRemains(userId, remainsIndex);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/hasspace")
    public ResponseEntity<Boolean> hasSpace(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer count) {
        boolean hasSpace = remainsService.hasRemainsSpace(userId, count);
        return ResponseEntity.ok(hasSpace);
    }
}
