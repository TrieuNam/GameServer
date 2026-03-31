package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.model.entity.PetCloth;
import com.SouthMillion.pet_service.service.PetClothService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pet Cloth Controller
 * Handles pet clothing/skin operations
 */
@RestController
@RequestMapping("/api/pet/cloth")
@RequiredArgsConstructor
@Slf4j
public class PetClothController {

    private final PetClothService clothService;

    @PostMapping("/{userId}/upgrade")
    public ResponseEntity<Void> clothUp(
            @PathVariable String userId,
            @RequestParam Integer clothId,
            @RequestParam(defaultValue = "false") Boolean isDiamond) {
        log.info("POST /api/pet/cloth/{}/upgrade - clothId={}, isDiamond={}", 
            userId, clothId, isDiamond);
        clothService.clothUp(userId, clothId, isDiamond);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/wear")
    public ResponseEntity<Void> clothWear(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer clothId) {
        log.info("POST /api/pet/cloth/{}/wear - petIndex={}, clothId={}", 
            userId, petIndex, clothId);
        clothService.clothWear(userId, petIndex, clothId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/unequip")
    public ResponseEntity<Void> unequipCloth(
            @PathVariable String userId,
            @RequestParam Integer petIndex) {
        log.info("DELETE /api/pet/cloth/{}/unequip - petIndex={}", userId, petIndex);
        clothService.unequipCloth(userId, petIndex);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/{clothId}")
    public ResponseEntity<PetCloth> getCloth(
            @PathVariable String userId,
            @PathVariable Integer clothId) {
        log.debug("GET /api/pet/cloth/{}/{}", userId, clothId);
        PetCloth cloth = clothService.getCloth(userId, clothId);
        return ResponseEntity.ok(cloth);
    }

    @GetMapping("/{userId}/bonus/{clothId}")
    public ResponseEntity<Long> getClothBonus(
            @PathVariable String userId,
            @PathVariable Integer clothId) {
        Long bonus = clothService.calculateClothBonus(userId, clothId);
        return ResponseEntity.ok(bonus);
    }
}
