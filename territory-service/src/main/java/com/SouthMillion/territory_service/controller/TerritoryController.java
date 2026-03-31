package com.SouthMillion.territory_service.controller;

import com.SouthMillion.territory_service.model.entity.Territory;
import com.SouthMillion.territory_service.model.entity.TerritoryBuilding;
import com.SouthMillion.territory_service.service.TerritoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Territory Controller
 * Territory/Base management operations
 */
@RestController
@RequestMapping("/api/territory")
@RequiredArgsConstructor
@Slf4j
public class TerritoryController {
    
    private final TerritoryService territoryService;
    
    // Territory operations
    @GetMapping("/{userId}")
    public ResponseEntity<Territory> getTerritory(@PathVariable Long userId) {
        log.debug("GET /api/territory/{}", userId);
        Territory territory = territoryService.getTerritory(userId);
        return ResponseEntity.ok(territory);
    }
    
    @PostMapping("/{userId}/create")
    public ResponseEntity<Territory> createTerritory(
            @PathVariable Long userId,
            @RequestParam Integer territoryId) {
        log.info("POST /api/territory/{}/create - territoryId={}", userId, territoryId);
        Territory territory = territoryService.createTerritory(userId, territoryId);
        return ResponseEntity.ok(territory);
    }
    
    @PostMapping("/{userId}/levelup")
    public ResponseEntity<Territory> levelUpTerritory(@PathVariable Long userId) {
        log.info("POST /api/territory/{}/levelup", userId);
        Territory territory = territoryService.levelUpTerritory(userId);
        return ResponseEntity.ok(territory);
    }
    
    @PutMapping("/{userId}/rename")
    public ResponseEntity<Territory> renameTerritory(
            @PathVariable Long userId,
            @RequestParam String name) {
        log.info("PUT /api/territory/{}/rename - name={}", userId, name);
        Territory territory = territoryService.renameTerritory(userId, name);
        return ResponseEntity.ok(territory);
    }
    
    @PutMapping("/{userId}/appearance")
    public ResponseEntity<Territory> changeAppearance(
            @PathVariable Long userId,
            @RequestParam Integer appearanceId) {
        log.info("PUT /api/territory/{}/appearance - appearanceId={}", userId, appearanceId);
        Territory territory = territoryService.changeAppearance(userId, appearanceId);
        return ResponseEntity.ok(territory);
    }
    
    @PostMapping("/{userId}/collect")
    public ResponseEntity<Territory> collectResources(@PathVariable Long userId) {
        log.info("POST /api/territory/{}/collect", userId);
        Territory territory = territoryService.collectResources(userId);
        return ResponseEntity.ok(territory);
    }
    
    @PostMapping("/{userId}/update-production")
    public ResponseEntity<Void> updateProduction(@PathVariable Long userId) {
        log.debug("POST /api/territory/{}/update-production", userId);
        territoryService.updateProduction(userId);
        return ResponseEntity.ok().build();
    }
    
    // Building operations
    @GetMapping("/{userId}/buildings")
    public ResponseEntity<List<TerritoryBuilding>> getAllBuildings(@PathVariable Long userId) {
        log.debug("GET /api/territory/{}/buildings", userId);
        List<TerritoryBuilding> buildings = territoryService.getAllBuildings(userId);
        return ResponseEntity.ok(buildings);
    }
    
    @GetMapping("/{userId}/buildings/{slotId}")
    public ResponseEntity<TerritoryBuilding> getBuilding(
            @PathVariable Long userId,
            @PathVariable Integer slotId) {
        log.debug("GET /api/territory/{}/buildings/{}", userId, slotId);
        TerritoryBuilding building = territoryService.getBuilding(userId, slotId);
        return ResponseEntity.ok(building);
    }
    
    @PostMapping("/{userId}/buildings/{slotId}/construct")
    public ResponseEntity<TerritoryBuilding> constructBuilding(
            @PathVariable Long userId,
            @PathVariable Integer slotId,
            @RequestParam Integer buildingId) {
        log.info("POST /api/territory/{}/buildings/{}/construct - buildingId={}", 
            userId, slotId, buildingId);
        TerritoryBuilding building = territoryService.constructBuilding(userId, slotId, buildingId);
        return ResponseEntity.ok(building);
    }
    
    @PostMapping("/{userId}/buildings/{slotId}/upgrade")
    public ResponseEntity<TerritoryBuilding> upgradeBuilding(
            @PathVariable Long userId,
            @PathVariable Integer slotId) {
        log.info("POST /api/territory/{}/buildings/{}/upgrade", userId, slotId);
        TerritoryBuilding building = territoryService.upgradeBuilding(userId, slotId);
        return ResponseEntity.ok(building);
    }
    
    @PostMapping("/{userId}/buildings/{slotId}/finish")
    public ResponseEntity<TerritoryBuilding> finishConstruction(
            @PathVariable Long userId,
            @PathVariable Integer slotId) {
        log.info("POST /api/territory/{}/buildings/{}/finish", userId, slotId);
        TerritoryBuilding building = territoryService.finishConstruction(userId, slotId);
        return ResponseEntity.ok(building);
    }
    
    @PostMapping("/{userId}/buildings/{slotId}/instant-finish")
    public ResponseEntity<TerritoryBuilding> instantFinish(
            @PathVariable Long userId,
            @PathVariable Integer slotId) {
        log.info("POST /api/territory/{}/buildings/{}/instant-finish", userId, slotId);
        TerritoryBuilding building = territoryService.instantFinish(userId, slotId);
        return ResponseEntity.ok(building);
    }
    
    @DeleteMapping("/{userId}/buildings/{slotId}")
    public ResponseEntity<Void> demolishBuilding(
            @PathVariable Long userId,
            @PathVariable Integer slotId) {
        log.info("DELETE /api/territory/{}/buildings/{}", userId, slotId);
        territoryService.demolishBuilding(userId, slotId);
        return ResponseEntity.ok().build();
    }
    
    // Query operations
    @GetMapping("/{userId}/buildings/completed")
    public ResponseEntity<List<TerritoryBuilding>> getCompletedConstructions(@PathVariable Long userId) {
        log.debug("GET /api/territory/{}/buildings/completed", userId);
        List<TerritoryBuilding> buildings = territoryService.getCompletedConstructions(userId);
        return ResponseEntity.ok(buildings);
    }
    
    @GetMapping("/{userId}/stats/defense")
    public ResponseEntity<Long> getTotalDefense(@PathVariable Long userId) {
        Long defense = territoryService.getTotalDefense(userId);
        return ResponseEntity.ok(defense);
    }
    
    @GetMapping("/{userId}/stats/attack")
    public ResponseEntity<Long> getTotalAttack(@PathVariable Long userId) {
        Long attack = territoryService.getTotalAttack(userId);
        return ResponseEntity.ok(attack);
    }
    
    @GetMapping("/{userId}/stats/prosperity")
    public ResponseEntity<Long> getTotalProsperity(@PathVariable Long userId) {
        Long prosperity = territoryService.getTotalProsperity(userId);
        return ResponseEntity.ok(prosperity);
    }
    
    // Validation
    @GetMapping("/{userId}/can-levelup")
    public ResponseEntity<Boolean> canLevelUp(@PathVariable Long userId) {
        boolean can = territoryService.canLevelUp(userId);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{userId}/buildings/{slotId}/can-construct")
    public ResponseEntity<Boolean> canConstruct(
            @PathVariable Long userId,
            @PathVariable Integer slotId) {
        boolean can = territoryService.canConstruct(userId, slotId);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{userId}/buildings/{slotId}/can-upgrade")
    public ResponseEntity<Boolean> canUpgradeBuilding(
            @PathVariable Long userId,
            @PathVariable Integer slotId) {
        boolean can = territoryService.canUpgradeBuilding(userId, slotId);
        return ResponseEntity.ok(can);
    }
}
