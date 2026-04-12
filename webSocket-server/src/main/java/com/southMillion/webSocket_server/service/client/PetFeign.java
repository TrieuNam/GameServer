package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for pet-service
 *
 * Khớp với PetController endpoints hiện tại (/api/pet/{userId}/...):
 *
 * Thay đổi so với phiên bản cũ:
 *   - activate/{petTemplateId}  → add         (petTemplateId chuyển vào body)
 *   - upgrade                  → levelup
 *   - evolve/{petId}           → evolve       (petId chuyển vào body)
 *   - setactive/{petId}        → fight        (petId chuyển vào body)
 *   - feedPet / evolvePet(body) đã bị xoá
 */
@FeignClient(name = "pet-service")
public interface PetFeign {

    /** GET /api/pet/{roleId} – lấy toàn bộ thú của người chơi */
    @GetMapping("/api/pet/{roleId}")
    Map<String, Object> getRolePets(@PathVariable("roleId") String roleId);

    /** POST /api/pet/gem/{roleId}/onekey – one-key normal pet gem level-up */
    @PostMapping("/api/pet/gem/{roleId}/onekey")
    Void oneKeyGemLevelUp(
            @PathVariable("roleId") String roleId,
            @RequestParam("petIndex") Integer petIndex,
            @RequestParam("slotIndex") Integer slotIndex
    );

    /** POST /api/pet/gem/{roleId}/tsgem/onekey – one-key special pet gem level-up */
    @PostMapping("/api/pet/gem/{roleId}/tsgem/onekey")
    Void oneKeyTSGemLevelUp(
            @PathVariable("roleId") String roleId,
            @RequestParam("gemIndex") Integer gemIndex
    );

    /**
     * POST /api/pet/{roleId}/add – mở khoá / thêm thú mới.
     * Body: {petTemplateId, ...}
     * (đổi từ /activate/{petTemplateId} → /add với petTemplateId trong body)
     */
    @PostMapping("/api/pet/{roleId}/add")
    Map<String, Object> activatePet(
        @PathVariable("roleId") String roleId,
        @RequestBody Map<String, Object> request
    );

    /**
     * POST /api/pet/{roleId}/levelup – nâng cấp cấp độ thú.
     * Body: {petIndex, materialIds[]}
     * (đổi từ /upgrade → /levelup)
     */
    @PostMapping("/api/pet/{roleId}/levelup")
    Map<String, Object> upgradePet(
        @PathVariable("roleId") String roleId,
        @RequestBody Map<String, Object> request
    );

    /**
     * POST /api/pet/{roleId}/evolve – tiến hóa thú (tăng sao).
     * Body: {petIndex, ...}
     * (đổi từ /evolve/{petId} → /evolve với petId trong body)
     */
    @PostMapping("/api/pet/{roleId}/evolve")
    Map<String, Object> evolvePet(
        @PathVariable("roleId") String roleId,
        @RequestBody Map<String, Object> request
    );

    /**
     * POST /api/pet/{roleId}/fight – đặt thú chiến đấu (set active).
     * Body: {petIndex, ...}
     * (đổi từ /setactive/{petId} → /fight)
     */
    @PostMapping("/api/pet/{roleId}/fight")
    Map<String, Object> setActivePet(
        @PathVariable("roleId") String roleId,
        @RequestBody Map<String, Object> request
    );

    @PostMapping("/api/pet/{roleId}/treasure")
    Map<String, Object> drawTreasure(
            @PathVariable("roleId") String roleId,
            @RequestParam("type") Integer type
    );

    // === Pet Dungeon ===

    /** GET /api/pet/{roleId}/dungeon – thông tin pet dungeon */
    @GetMapping("/api/pet/{roleId}/dungeon")
    Map<String, Object> getPetDungeonInfo(@PathVariable("roleId") String roleId);

    /** POST /api/pet/{roleId}/dungeon/start – bắt đầu pet dungeon */
    @PostMapping("/api/pet/{roleId}/dungeon/start")
    Map<String, Object> startPetDungeon(
            @PathVariable("roleId") String roleId,
            @RequestParam("dungeonId") Integer dungeonId);

    /** POST /api/pet/{roleId}/dungeon/claim – nhận thưởng pet dungeon */
    @PostMapping("/api/pet/{roleId}/dungeon/claim")
    Map<String, Object> claimPetDungeonReward(
            @PathVariable("roleId") String roleId,
            @RequestParam("dungeonId") Integer dungeonId);
}
