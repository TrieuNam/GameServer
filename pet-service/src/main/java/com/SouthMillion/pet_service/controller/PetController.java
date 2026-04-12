package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.model.dto.*;
import com.SouthMillion.pet_service.model.entity.Pet;
import com.SouthMillion.pet_service.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pet REST Controller
 * Provides HTTP API for pet management
 */
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
@Slf4j
public class PetController {

    private final PetService petService;
    private final PetGemService gemService;
    private final PetTSGemService tsGemService;
    private final PetClothService clothService;
    private final PetRemainsService remainsService;

    /**
     * Get all pet information for a user
     * Called on user login
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getAllPetInfo(@PathVariable String userId) {
        log.debug("GET /api/pet/{}", userId);
        PetAllInfoResponse info = petService.getAllPetInfo(userId);

        List<PetDataDTO> petList = info.getPetList() != null ? info.getPetList() : List.of();
        List<Integer> fightPetIndex = info.getFightPetIndex() != null ? info.getFightPetIndex() : List.of();
        List<TSGemDataDTO> tsGemList = info.getTsGemList() != null ? info.getTsGemList() : List.of();
        List<ClothDataDTO> clothList = info.getClothList() != null ? info.getClothList() : List.of();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("hasData", !petList.isEmpty() || !tsGemList.isEmpty() || !clothList.isEmpty());
        response.put("fightPetIndex", fightPetIndex);
        response.put("petList", petList);
        response.put("pets", petList);
        response.put("tsGemList", tsGemList);
        response.put("clothList", clothList);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a new pet to user's collection
     */
    @PostMapping("/{userId}/add")
    public ResponseEntity<Pet> addPet(
            @PathVariable String userId,
            @RequestParam Integer petId) {
        log.info("POST /api/pet/{}/add - petId={}", userId, petId);
        Pet pet = petService.addPet(userId, petId);
        return ResponseEntity.ok(pet);
    }

    @PostMapping("/{userId}/treasure")
    public ResponseEntity<Map<String, Object>> drawTreasure(
            @PathVariable String userId,
            @RequestParam Integer type) {
        log.info("POST /api/pet/{}/treasure - type={}", userId, type);
        return ResponseEntity.ok(petService.drawTreasure(userId, type));
    }

    /**
     * Level up a pet
     */
    @PostMapping("/{userId}/levelup")
    public ResponseEntity<Void> levelUp(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam(defaultValue = "1") Integer num) {
        log.info("POST /api/pet/{}/levelup - petIndex={}, num={}", userId, petIndex, num);
        petService.levelUp(userId, petIndex, num);
        return ResponseEntity.ok().build();
    }

    /**
     * Grade up (awaken) a pet
     */
    @PostMapping("/{userId}/gradeup")
    public ResponseEntity<Void> gradeUp(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/{}/gradeup - petIndex={}, materials={}", 
            userId, petIndex, materials);
        petService.gradeUp(userId, petIndex, materials);
        return ResponseEntity.ok().build();
    }

    /**
     * Evolve a pet to new type
     */
    @PostMapping("/{userId}/evolve")
    public ResponseEntity<Void> evolve(
            @PathVariable String userId,
            @RequestParam Integer petIndex) {
        log.info("POST /api/pet/{}/evolve - petIndex={}", userId, petIndex);
        petService.evolve(userId, petIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Learn or replace a skill
     */
    @PostMapping("/{userId}/skill/learn")
    public ResponseEntity<Void> learnSkill(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer skillIndex,
            @RequestParam Integer skillItemId) {
        log.info("POST /api/pet/{}/skill/learn - petIndex={}, skillIndex={}, skillItemId={}", 
            userId, petIndex, skillIndex, skillItemId);
        petService.learnSkill(userId, petIndex, skillIndex, skillItemId);
        return ResponseEntity.ok().build();
    }

    /**
     * Unlock a skill slot
     */
    @PostMapping("/{userId}/skill/unlock")
    public ResponseEntity<Void> unlockSkill(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer skillSeq) {
        log.info("POST /api/pet/{}/skill/unlock - petIndex={}, skillSeq={}", 
            userId, petIndex, skillSeq);
        petService.unlockSkill(userId, petIndex, skillSeq);
        return ResponseEntity.ok().build();
    }

    /**
     * Lock/unlock skill slots
     */
    @PostMapping("/{userId}/skill/lock")
    public ResponseEntity<Void> lockSkill(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer lockFlag) {
        log.info("POST /api/pet/{}/skill/lock - petIndex={}, lockFlag={}", 
            userId, petIndex, lockFlag);
        petService.lockSkill(userId, petIndex, lockFlag);
        return ResponseEntity.ok().build();
    }

    /**
     * Set active fighting pet
     */
    @PostMapping("/{userId}/fight")
    public ResponseEntity<Void> setFightPet(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam(defaultValue = "0") Integer fightIndex) {
        log.info("POST /api/pet/{}/fight - petIndex={}, fightIndex={}", 
            userId, petIndex, fightIndex);
        petService.setFightPet(userId, petIndex, fightIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Discard (release) a pet
     */
    @DeleteMapping("/{userId}/{petIndex}")
    public ResponseEntity<Void> discardPet(
            @PathVariable String userId,
            @PathVariable Integer petIndex) {
        log.info("DELETE /api/pet/{}/{}", userId, petIndex);
        petService.discardPet(userId, petIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Get pet capability (combat power)
     */
    @GetMapping("/{userId}/capability/{petIndex}")
    public ResponseEntity<Long> getCapability(
            @PathVariable String userId,
            @PathVariable Integer petIndex) {
        log.debug("GET /api/pet/{}/capability/{}", userId, petIndex);
        Long capability = petService.calculateCapability(userId, petIndex);
        return ResponseEntity.ok(capability);
    }

    /**
     * Recalculate all pet stats
     */
    @PostMapping("/{userId}/recalculate")
    public ResponseEntity<Void> recalculateStats(@PathVariable String userId) {
        log.info("POST /api/pet/{}/recalculate", userId);
        petService.recalculateAllStats(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get a specific pet
     */
    @GetMapping("/{userId}/{petIndex}")
    public ResponseEntity<Pet> getPet(
            @PathVariable String userId,
            @PathVariable Integer petIndex) {
        log.debug("GET /api/pet/{}/{}", userId, petIndex);
        Pet pet = petService.getPet(userId, petIndex);
        return ResponseEntity.ok(pet);
    }

    /**
     * Check if user has space for new pets
     */
    @GetMapping("/{userId}/hasspace")
    public ResponseEntity<Boolean> hasSpace(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer count) {
        boolean hasSpace = petService.hasSpace(userId, count);
        return ResponseEntity.ok(hasSpace);
    }

    // ============================================
    // GEM OPERATIONS
    // ============================================

    /**
     * Inlay (equip) a normal gem on pet
     */
    @PostMapping("/{userId}/gem/inlay")
    public ResponseEntity<Void> inlayGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex,
            @RequestParam Integer gemItemId) {
        log.info("POST /api/pet/{}/gem/inlay - petIndex={}, slot={}, gemId={}", 
            userId, petIndex, slotIndex, gemItemId);
        gemService.inlayGem(userId, petIndex, slotIndex, gemItemId);
        return ResponseEntity.ok().build();
    }

    /**
     * Dismount (unequip) a gem from pet
     */
    @PostMapping("/{userId}/gem/dismount")
    public ResponseEntity<Void> dismountGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex) {
        log.info("POST /api/pet/{}/gem/dismount - petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        gemService.dismountGem(userId, petIndex, slotIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Level up a gem in bag (not equipped)
     */
    @PostMapping("/{userId}/gem/levelup-bag")
    public ResponseEntity<Void> gemLevelUpBag(
            @PathVariable String userId,
            @RequestParam Integer itemId,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/{}/gem/levelup-bag - itemId={}, materials={}", 
            userId, itemId, materials);
        gemService.gemLevelUpBag(userId, itemId, materials);
        return ResponseEntity.ok().build();
    }

    /**
     * Level up an equipped gem
     */
    @PostMapping("/{userId}/gem/levelup-pet")
    public ResponseEntity<Void> gemLevelUpPet(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/{}/gem/levelup-pet - petIndex={}, slot={}, materials={}", 
            userId, petIndex, slotIndex, materials);
        gemService.gemLevelUpPet(userId, petIndex, slotIndex, materials);
        return ResponseEntity.ok().build();
    }

    /**
     * One-key gem level up (auto upgrade)
     */
    @PostMapping("/{userId}/gem/onekey-levelup")
    public ResponseEntity<Void> oneKeyGemLevelUp(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex) {
        log.info("POST /api/pet/{}/gem/onekey-levelup - petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        gemService.oneKeyGemLevelUp(userId, petIndex, slotIndex);
        return ResponseEntity.ok().build();
    }

    // ============================================
    // SPECIAL GEM (TS GEM) OPERATIONS
    // ============================================

    /**
     * Inlay (equip) a special gem on pet
     */
    @PostMapping("/{userId}/tsgem/inlay")
    public ResponseEntity<Void> inlayTSGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex,
            @RequestParam Integer gemIndex) {
        log.info("POST /api/pet/{}/tsgem/inlay - petIndex={}, slot={}, gemIndex={}", 
            userId, petIndex, slotIndex, gemIndex);
        tsGemService.inlayTSGem(userId, petIndex, slotIndex, gemIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Dismount (unequip) a special gem from pet
     */
    @PostMapping("/{userId}/tsgem/dismount")
    public ResponseEntity<Void> dismountTSGem(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer slotIndex) {
        log.info("POST /api/pet/{}/tsgem/dismount - petIndex={}, slot={}", 
            userId, petIndex, slotIndex);
        tsGemService.dismountTSGem(userId, petIndex, slotIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Level up a special gem
     */
    @PostMapping("/{userId}/tsgem/levelup")
    public ResponseEntity<Void> tsGemLevelUp(
            @PathVariable String userId,
            @RequestParam Integer gemIndex,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/{}/tsgem/levelup - gemIndex={}, materials={}", 
            userId, gemIndex, materials);
        tsGemService.tsGemLevelUp(userId, gemIndex, materials);
        return ResponseEntity.ok().build();
    }

    /**
     * One-key special gem level up (auto upgrade)
     */
    @PostMapping("/{userId}/tsgem/onekey-levelup")
    public ResponseEntity<Void> oneKeyTSGemLevelUp(
            @PathVariable String userId,
            @RequestParam Integer gemIndex) {
        log.info("POST /api/pet/{}/tsgem/onekey-levelup - gemIndex={}", userId, gemIndex);
        tsGemService.oneKeyTSGemLevelUp(userId, gemIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Refresh special gem attributes
     */
    @PostMapping("/{userId}/tsgem/refresh")
    public ResponseEntity<Void> tsGemRefresh(
            @PathVariable String userId,
            @RequestParam Integer gemIndex,
            @RequestParam Integer lockFlag) {
        log.info("POST /api/pet/{}/tsgem/refresh - gemIndex={}, lockFlag={}", 
            userId, gemIndex, lockFlag);
        tsGemService.tsGemRefresh(userId, gemIndex, lockFlag);
        return ResponseEntity.ok().build();
    }

    /**
     * Add attribute to special gem
     */
    @PostMapping("/{userId}/tsgem/addattr")
    public ResponseEntity<Void> addTSGemAttr(
            @PathVariable String userId,
            @RequestParam Integer gemIndex) {
        log.info("POST /api/pet/{}/tsgem/addattr - gemIndex={}", userId, gemIndex);
        tsGemService.addTSGemAttr(userId, gemIndex);
        return ResponseEntity.ok().build();
    }

    // ============================================
    // CLOTHING OPERATIONS
    // ============================================

    /**
     * Upgrade clothing level
     */
    @PostMapping("/{userId}/cloth/upgrade")
    public ResponseEntity<Void> clothUp(
            @PathVariable String userId,
            @RequestParam Integer clothId,
            @RequestParam(defaultValue = "false") boolean isDiamond) {
        log.info("POST /api/pet/{}/cloth/upgrade - clothId={}, isDiamond={}", 
            userId, clothId, isDiamond);
        clothService.clothUp(userId, clothId, isDiamond);
        return ResponseEntity.ok().build();
    }

    /**
     * Wear clothing on pet
     */
    @PostMapping("/{userId}/cloth/wear")
    public ResponseEntity<Void> clothWear(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer clothId) {
        log.info("POST /api/pet/{}/cloth/wear - petIndex={}, clothId={}", 
            userId, petIndex, clothId);
        clothService.clothWear(userId, petIndex, clothId);
        return ResponseEntity.ok().build();
    }

    /**
     * Remove clothing from pet
     */
    @PostMapping("/{userId}/cloth/unequip")
    public ResponseEntity<Void> clothUnequip(
            @PathVariable String userId,
            @RequestParam Integer petIndex) {
        log.info("POST /api/pet/{}/cloth/unequip - petIndex={}", userId, petIndex);
        clothService.unequipCloth(userId, petIndex);
        return ResponseEntity.ok().build();
    }

    // ============================================
    // REMAINS (RELICS) OPERATIONS
    // ============================================

    /**
     * Equip relic on pet
     */
    @PostMapping("/{userId}/remains/equip")
    public ResponseEntity<Void> equipRemains(
            @PathVariable String userId,
            @RequestParam Integer petIndex,
            @RequestParam Integer remainsId) {
        log.info("POST /api/pet/{}/remains/equip - petIndex={}, remainsId={}", 
            userId, petIndex, remainsId);
        remainsService.equipRemains(userId, petIndex, remainsId);
        return ResponseEntity.ok().build();
    }

    /**
     * Unequip relic from pet
     */
    @PostMapping("/{userId}/remains/unequip")
    public ResponseEntity<Void> unequipRemains(
            @PathVariable String userId,
            @RequestParam Integer petIndex) {
        log.info("POST /api/pet/{}/remains/unequip - petIndex={}", userId, petIndex);
        remainsService.unequipRemains(userId, petIndex);
        return ResponseEntity.ok().build();
    }

    /**
     * Upgrade relic level
     */
    @PostMapping("/{userId}/remains/upgrade")
    public ResponseEntity<Void> upgradeRemains(
            @PathVariable String userId,
            @RequestParam Integer remainsId,
            @RequestBody List<Integer> materials) {
        log.info("POST /api/pet/{}/remains/upgrade - remainsId={}, materials={}", 
            userId, remainsId, materials);
        remainsService.upgradeRemains(userId, remainsId, materials);
        return ResponseEntity.ok().build();
    }

    /**
     * Get pet dungeon info
     */
    @GetMapping("/{userId}/dungeon")
    public ResponseEntity<java.util.Map<String, Object>> getPetDungeonInfo(
            @PathVariable String userId) {
        log.info("GET /api/pet/{}/dungeon", userId);
        return ResponseEntity.ok(petService.getPetDungeonInfo(userId));
    }

    /**
     * Start pet dungeon challenge
     */
    @PostMapping("/{userId}/dungeon/start")
    public ResponseEntity<java.util.Map<String, Object>> startPetDungeon(
            @PathVariable String userId,
            @RequestParam Integer dungeonId) {
        log.info("POST /api/pet/{}/dungeon/start - dungeonId={}", userId, dungeonId);
        return ResponseEntity.ok(petService.startPetDungeon(userId, dungeonId));
    }

    /**
     * Claim pet dungeon reward
     */
    @PostMapping("/{userId}/dungeon/claim")
    public ResponseEntity<java.util.Map<String, Object>> claimPetDungeonReward(
            @PathVariable String userId,
            @RequestParam Integer dungeonId) {
        log.info("POST /api/pet/{}/dungeon/claim - dungeonId={}", userId, dungeonId);
        return ResponseEntity.ok(petService.claimPetDungeonReward(userId, dungeonId));
    }
}

