package com.SouthMillion.trial_service.controller;

import com.SouthMillion.trial_service.model.entity.TrialRecord;
import com.SouthMillion.trial_service.service.TrialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Trial Controller - WebSocket/Feign Compatible
 * Trial/Challenge dungeon operations
 * Updated to match TrialFeign interface expectations
 */
@RestController
@RequestMapping("/api/trial")
@RequiredArgsConstructor
@Slf4j
public class TrialController {
    
    private final TrialService trialService;
    
    @GetMapping("/{roleId}")
    public ResponseEntity<List<TrialRecord>> getAllRecords(@PathVariable Long roleId) {
        log.debug("GET /api/trial/{}", roleId);
        List<TrialRecord> records = trialService.getAllRecords(roleId);
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/{roleId}/{trialId}")
    public ResponseEntity<TrialRecord> getRecord(
            @PathVariable Long roleId,
            @PathVariable Integer trialId) {
        log.debug("GET /api/trial/{}/{}", roleId, trialId);
        TrialRecord record = trialService.getRecord(roleId, trialId);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/{roleId}/start/{trialId}")
    public ResponseEntity<TrialRecord> startTrial(
            @PathVariable Long roleId,
            @PathVariable Integer trialId) {
        log.info("POST /api/trial/{}/start/{}", roleId, trialId);
        TrialRecord record = trialService.startTrial(roleId, trialId);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/{roleId}/complete/{trialId}")
    public ResponseEntity<TrialRecord> completeTrial(
            @PathVariable Long roleId,
            @PathVariable Integer trialId,
            @RequestBody Map<String, Object> request) {
        Long score = ((Number) request.get("score")).longValue();
        Integer stars = ((Number) request.get("stars")).intValue();
        Integer completionTime = request.get("completionTime") != null 
            ? ((Number) request.get("completionTime")).intValue() 
            : null;
        log.info("POST /api/trial/{}/complete/{} - score={}, stars={}, time={}", 
            roleId, trialId, score, stars, completionTime);
        TrialRecord record = trialService.completeTrial(roleId, trialId, score, stars, completionTime);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/{roleId}/fail/{trialId}")
    public ResponseEntity<TrialRecord> failTrial(
            @PathVariable Long roleId,
            @PathVariable Integer trialId) {
        log.info("POST /api/trial/{}/fail/{}", roleId, trialId);
        TrialRecord record = trialService.failTrial(roleId, trialId);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/{roleId}/advance/{trialId}")
    public ResponseEntity<TrialRecord> advanceStage(
            @PathVariable Long roleId,
            @PathVariable Integer trialId) {
        log.info("POST /api/trial/{}/advance/{}", roleId, trialId);
        TrialRecord record = trialService.advanceStage(roleId, trialId);
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/{roleId}/reset/{trialId}")
    public ResponseEntity<Void> resetProgress(
            @PathVariable Long roleId,
            @PathVariable Integer trialId) {
        log.info("POST /api/trial/{}/reset/{}", roleId, trialId);
        trialService.resetProgress(roleId, trialId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{roleId}/claim/{trialId}/{stageId}")
    public ResponseEntity<Boolean> claimReward(
            @PathVariable Long roleId,
            @PathVariable Integer trialId,
            @PathVariable Integer stageId) {
        log.info("POST /api/trial/{}/claim/{}/{}", roleId, trialId, stageId);
        boolean success = trialService.claimReward(roleId, trialId, stageId);
        return ResponseEntity.ok(success);
    }
    
    @GetMapping("/{roleId}/{trialId}/claimed")
    public ResponseEntity<List<Integer>> getClaimedRewards(
            @PathVariable Long roleId,
            @PathVariable Integer trialId) {
        List<Integer> claimed = trialService.getClaimedRewards(roleId, trialId);
        return ResponseEntity.ok(claimed);
    }
    
    @GetMapping("/{roleId}/{trialId}/best")
    public ResponseEntity<Map<String, Object>> getBestRecord(
            @PathVariable Long roleId,
            @PathVariable Integer trialId) {
        log.debug("GET /api/trial/{}/{}/best", roleId, trialId);
        Long bestScore = trialService.getBestScore(roleId, trialId);
        Integer bestStage = trialService.getBestStage(roleId, trialId);
        Integer bestTime = trialService.getBestTime(roleId, trialId);
        return ResponseEntity.ok(Map.of(
            "bestScore", bestScore,
            "bestStage", bestStage,
            "bestTime", bestTime
        ));
    }
}
