package com.SouthMillion.escort_service.controller;

import com.SouthMillion.escort_service.model.entity.EscortMission;
import com.SouthMillion.escort_service.model.entity.EscortStats;
import com.SouthMillion.escort_service.service.EscortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Escort Controller
 * Escort mission operations
 */
@RestController
@RequestMapping("/api/escort")
@RequiredArgsConstructor
@Slf4j
public class EscortController {
    
    private final EscortService escortService;
    
    // Feign-compatible endpoints (roleId = userId in this service)
    @GetMapping("/player/{roleId}")
    public ResponseEntity<List<EscortMission>> getEscortInfo(@PathVariable String roleId) {
        log.debug("GET /api/escort/player/{}", roleId);
        List<EscortMission> missions = escortService.getAllMissions(roleId);
        return ResponseEntity.ok(missions);
    }

    @GetMapping("/targets/{roleId}")
    public ResponseEntity<List<EscortMission>> getEscortTargets(@PathVariable String roleId) {
        log.debug("GET /api/escort/targets/{}", roleId);
        List<EscortMission> missions = escortService.getActiveMissions(roleId);
        return ResponseEntity.ok(missions);
    }

    @GetMapping("/history/{roleId}")
    public ResponseEntity<List<EscortMission>> getEscortHistory(
            @PathVariable String roleId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        log.debug("GET /api/escort/history/{}", roleId);
        List<EscortMission> missions = escortService.getCompletedMissions(roleId);
        return ResponseEntity.ok(missions);
    }

    @PostMapping("/start")
    public ResponseEntity<EscortMission> startEscort(@RequestBody java.util.Map<String, Object> request) {
        String roleId = request.get("roleId").toString();
        Integer escortType = (Integer) request.getOrDefault("escortType", 1);
        log.info("POST /api/escort/start roleId={}", roleId);
        EscortMission mission = escortService.generateMission(roleId, escortType);
        return ResponseEntity.ok(mission);
    }

    @PostMapping("/complete")
    public ResponseEntity<EscortMission> completeEscort(@RequestBody java.util.Map<String, Object> request) {
        String roleId = request.get("roleId").toString();
        Long escortId = Long.parseLong(request.get("escortId").toString());
        log.info("POST /api/escort/complete roleId={}", roleId);
        EscortMission mission = escortService.completeMission(roleId, escortId);
        return ResponseEntity.ok(mission);
    }

    @PostMapping("/rob")
    public ResponseEntity<java.util.Map<String, Object>> robEscort(@RequestBody java.util.Map<String, Object> request) {
        String userId   = request.getOrDefault("userId",   request.getOrDefault("roleId", "")).toString();
        String victimId = request.getOrDefault("victimId", "").toString();
        log.info("POST /api/escort/rob userId={} victimId={}", userId, victimId);
        if (userId.isBlank() || victimId.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "userId and victimId required"));
        }
        EscortMission intercepted = escortService.robEscort(userId, victimId);
        if (intercepted == null) {
            return ResponseEntity.ok(java.util.Map.of("success", false, "message", "victim_has_no_active_mission"));
        }
        return ResponseEntity.ok(java.util.Map.of("success", true, "mission", intercepted));
    }

    @PostMapping("/speedup")
    public ResponseEntity<java.util.Map<String, Object>> speedupEscort(@RequestBody java.util.Map<String, Object> request) {
        String userId    = request.getOrDefault("userId",    request.getOrDefault("roleId", "")).toString();
        long   missionId = Long.parseLong(request.getOrDefault("missionId", "0").toString());
        log.info("POST /api/escort/speedup userId={} missionId={}", userId, missionId);
        if (userId.isBlank() || missionId <= 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "userId and missionId required"));
        }
        EscortMission updated = escortService.speedupEscort(userId, missionId);
        return ResponseEntity.ok(java.util.Map.of("success", true, "mission", updated));
    }

    // Mission operations
    @GetMapping("/{userId}/missions")
    public ResponseEntity<List<EscortMission>> getAllMissions(@PathVariable String userId) {
        log.info("GET /api/escort/{}/missions", userId);
        List<EscortMission> missions = escortService.getAllMissions(userId);
        return ResponseEntity.ok(missions);
    }
    
    @GetMapping("/{userId}/missions/{missionId}")
    public ResponseEntity<EscortMission> getMission(
            @PathVariable String userId,
            @PathVariable Long missionId) {
        log.debug("GET /api/escort/{}/missions/{}", userId, missionId);
        EscortMission mission = escortService.getMission(userId, missionId);
        return ResponseEntity.ok(mission);
    }
    
    @PostMapping("/{userId}/missions/generate")
    public ResponseEntity<EscortMission> generateMission(
            @PathVariable String userId,
            @RequestParam Integer quality) {
        log.info("POST /api/escort/{}/missions/generate - quality={}", userId, quality);
        EscortMission mission = escortService.generateMission(userId, quality);
        return ResponseEntity.ok(mission);
    }
    
    @PostMapping("/{userId}/missions/{missionId}/start")
    public ResponseEntity<EscortMission> startMission(
            @PathVariable String userId,
            @PathVariable Long missionId) {
        log.info("POST /api/escort/{}/missions/{}/start", userId, missionId);
        EscortMission mission = escortService.startMission(userId, missionId);
        return ResponseEntity.ok(mission);
    }
    
    @PostMapping("/{userId}/missions/{missionId}/progress")
    public ResponseEntity<EscortMission> updateProgress(
            @PathVariable String userId,
            @PathVariable Long missionId,
            @RequestParam Integer increment) {
        log.info("POST /api/escort/{}/missions/{}/progress - increment={}", userId, missionId, increment);
        EscortMission mission = escortService.updateProgress(userId, missionId, increment);
        return ResponseEntity.ok(mission);
    }
    
    @PostMapping("/{userId}/missions/{missionId}/complete")
    public ResponseEntity<EscortMission> completeMission(
            @PathVariable String userId,
            @PathVariable Long missionId) {
        log.info("POST /api/escort/{}/missions/{}/complete", userId, missionId);
        EscortMission mission = escortService.completeMission(userId, missionId);
        return ResponseEntity.ok(mission);
    }
    
    @PostMapping("/{userId}/missions/{missionId}/fail")
    public ResponseEntity<EscortMission> failMission(
            @PathVariable String userId,
            @PathVariable Long missionId) {
        log.info("POST /api/escort/{}/missions/{}/fail", userId, missionId);
        EscortMission mission = escortService.failMission(userId, missionId);
        return ResponseEntity.ok(mission);
    }
    
    @DeleteMapping("/{userId}/missions/{missionId}")
    public ResponseEntity<Void> cancelMission(
            @PathVariable String userId,
            @PathVariable Long missionId) {
        log.info("DELETE /api/escort/{}/missions/{}", userId, missionId);
        escortService.cancelMission(userId, missionId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{userId}/missions/{missionId}/claim")
    public ResponseEntity<EscortMission> claimReward(
            @PathVariable String userId,
            @PathVariable Long missionId) {
        log.info("POST /api/escort/{}/missions/{}/claim", userId, missionId);
        EscortMission mission = escortService.claimReward(userId, missionId);
        return ResponseEntity.ok(mission);
    }
    
    // Mission management
    @PostMapping("/{userId}/missions/refresh")
    public ResponseEntity<List<EscortMission>> refreshMissions(@PathVariable String userId) {
        log.info("POST /api/escort/{}/missions/refresh", userId);
        List<EscortMission> missions = escortService.refreshMissions(userId);
        return ResponseEntity.ok(missions);
    }
    
    @PostMapping("/{userId}/missions/check-expired")
    public ResponseEntity<Void> checkExpiredMissions(@PathVariable String userId) {
        log.debug("POST /api/escort/{}/missions/check-expired", userId);
        escortService.checkExpiredMissions(userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{userId}/missions/active")
    public ResponseEntity<List<EscortMission>> getActiveMissions(@PathVariable String userId) {
        log.debug("GET /api/escort/{}/missions/active", userId);
        List<EscortMission> missions = escortService.getActiveMissions(userId);
        return ResponseEntity.ok(missions);
    }
    
    @GetMapping("/{userId}/missions/completed")
    public ResponseEntity<List<EscortMission>> getCompletedMissions(@PathVariable String userId) {
        log.debug("GET /api/escort/{}/missions/completed", userId);
        List<EscortMission> missions = escortService.getCompletedMissions(userId);
        return ResponseEntity.ok(missions);
    }
    
    @GetMapping("/{userId}/missions/unclaimed")
    public ResponseEntity<List<EscortMission>> getUnclaimedRewards(@PathVariable String userId) {
        log.debug("GET /api/escort/{}/missions/unclaimed", userId);
        List<EscortMission> missions = escortService.getUnclaimedRewards(userId);
        return ResponseEntity.ok(missions);
    }
    
    // Statistics operations
    @GetMapping("/{userId}/stats")
    public ResponseEntity<EscortStats> getStats(@PathVariable String userId) {
        log.debug("GET /api/escort/{}/stats", userId);
        EscortStats stats = escortService.getStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/{userId}/stats/init")
    public ResponseEntity<EscortStats> initializeStats(@PathVariable String userId) {
        log.info("POST /api/escort/{}/stats/init", userId);
        EscortStats stats = escortService.initializeStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/{userId}/stats/reset-daily")
    public ResponseEntity<Void> resetDailyStats(@PathVariable String userId) {
        log.info("POST /api/escort/{}/stats/reset-daily", userId);
        escortService.resetDailyStats(userId);
        return ResponseEntity.ok().build();
    }
    
    // Validation
    @GetMapping("/{userId}/can-start")
    public ResponseEntity<Boolean> canStartMission(@PathVariable String userId) {
        boolean can = escortService.canStartMission(userId);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{userId}/can-refresh")
    public ResponseEntity<Boolean> canRefresh(@PathVariable String userId) {
        boolean can = escortService.canRefresh(userId);
        return ResponseEntity.ok(can);
    }
    
    @GetMapping("/{userId}/has-active")
    public ResponseEntity<Boolean> hasActiveMission(@PathVariable String userId) {
        boolean has = escortService.hasActiveMission(userId);
        return ResponseEntity.ok(has);
    }
}
