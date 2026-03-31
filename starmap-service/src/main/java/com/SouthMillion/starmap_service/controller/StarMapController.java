package com.SouthMillion.starmap_service.controller;

import com.SouthMillion.starmap_service.model.entity.Constellation;
import com.SouthMillion.starmap_service.model.entity.Star;
import com.SouthMillion.starmap_service.service.StarMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Star Map Controller
 * Star map/constellation system operations
 * 
 * MsgIDs: 2150-2159
 */
@RestController
@RequestMapping("/api/starmap")
@RequiredArgsConstructor
@Slf4j
public class StarMapController {
    
    private final StarMapService starMapService;
    
    // Star endpoints
    @GetMapping("/{userId}/stars")
    public ResponseEntity<List<Star>> getAllStars(@PathVariable Long userId) {
        log.debug("GET /api/starmap/{}/stars", userId);
        List<Star> stars = starMapService.getAllStars(userId);
        return ResponseEntity.ok(stars);
    }
    
    @GetMapping("/{userId}/stars/{starId}")
    public ResponseEntity<Star> getStar(
            @PathVariable Long userId,
            @PathVariable Integer starId) {
        log.debug("GET /api/starmap/{}/stars/{}", userId, starId);
        Star star = starMapService.getStar(userId, starId);
        return ResponseEntity.ok(star);
    }
    
    @PostMapping("/{userId}/stars/{starId}/activate")
    public ResponseEntity<Star> activateStar(
            @PathVariable Long userId,
            @PathVariable Integer starId) {
        log.info("POST /api/starmap/{}/stars/{}/activate", userId, starId);
        Star star = starMapService.activateStar(userId, starId);
        return ResponseEntity.ok(star);
    }
    
    @PostMapping("/{userId}/stars/{starId}/levelup")
    public ResponseEntity<Star> levelUpStar(
            @PathVariable Long userId,
            @PathVariable Integer starId) {
        log.info("POST /api/starmap/{}/stars/{}/levelup", userId, starId);
        Star star = starMapService.levelUpStar(userId, starId);
        return ResponseEntity.ok(star);
    }
    
    @PostMapping("/{userId}/stars/{starId}/energy")
    public ResponseEntity<Star> addStarEnergy(
            @PathVariable Long userId,
            @PathVariable Integer starId,
            @RequestParam Long energy) {
        log.info("POST /api/starmap/{}/stars/{}/energy - energy={}", userId, starId, energy);
        Star star = starMapService.addStarEnergy(userId, starId, energy);
        return ResponseEntity.ok(star);
    }
    
    // Constellation endpoints
    @GetMapping("/{userId}/constellations")
    public ResponseEntity<List<Constellation>> getAllConstellations(@PathVariable Long userId) {
        log.debug("GET /api/starmap/{}/constellations", userId);
        List<Constellation> constellations = starMapService.getAllConstellations(userId);
        return ResponseEntity.ok(constellations);
    }
    
    @GetMapping("/{userId}/constellations/{constellationId}")
    public ResponseEntity<Constellation> getConstellation(
            @PathVariable Long userId,
            @PathVariable Integer constellationId) {
        log.debug("GET /api/starmap/{}/constellations/{}", userId, constellationId);
        Constellation constellation = starMapService.getConstellation(userId, constellationId);
        return ResponseEntity.ok(constellation);
    }
    
    @PostMapping("/{userId}/constellations/{constellationId}/unlock")
    public ResponseEntity<Constellation> unlockConstellation(
            @PathVariable Long userId,
            @PathVariable Integer constellationId) {
        log.info("POST /api/starmap/{}/constellations/{}/unlock", userId, constellationId);
        Constellation constellation = starMapService.unlockConstellation(userId, constellationId);
        return ResponseEntity.ok(constellation);
    }
    
    @PostMapping("/{userId}/constellations/{constellationId}/levelup")
    public ResponseEntity<Constellation> levelUpConstellation(
            @PathVariable Long userId,
            @PathVariable Integer constellationId) {
        log.info("POST /api/starmap/{}/constellations/{}/levelup", userId, constellationId);
        Constellation constellation = starMapService.levelUpConstellation(userId, constellationId);
        return ResponseEntity.ok(constellation);
    }
    
    @PostMapping("/{userId}/constellations/{constellationId}/check")
    public ResponseEntity<Void> checkCompletion(
            @PathVariable Long userId,
            @PathVariable Integer constellationId) {
        log.info("POST /api/starmap/{}/constellations/{}/check", userId, constellationId);
        starMapService.checkConstellationCompletion(userId, constellationId);
        return ResponseEntity.ok().build();
    }
    
    // Power calculations
    @GetMapping("/{userId}/power")
    public ResponseEntity<Long> getTotalPower(@PathVariable Long userId) {
        Long power = starMapService.calculateTotalStarMapPower(userId);
        return ResponseEntity.ok(power);
    }
    
    @GetMapping("/{userId}/constellations/{constellationId}/power")
    public ResponseEntity<Long> getConstellationPower(
            @PathVariable Long userId,
            @PathVariable Integer constellationId) {
        Constellation constellation = starMapService.getConstellation(userId, constellationId);
        Long power = starMapService.calculateConstellationPower(constellation);
        return ResponseEntity.ok(power);
    }
}
