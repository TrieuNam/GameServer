package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Trial Service Feign Client
 * 
 * Backend Service: trial-service
 * Manages trial/dungeon challenge system operations:
 * - Trial entry, completion, failure
 * - Stage progression and reward claiming
 * - Daily attempts management
 * - Score and star tracking
 */
@FeignClient(name = "trial-service")
public interface TrialFeign {

    /**
     * Get all trial records for role
     * @param roleId Role ID
     * @return Trial data: trials[], current_progress, daily_attempts_remaining
     */
    @GetMapping("/api/trial/{roleId}")
    Map<String, Object> getTrialData(@PathVariable("roleId") Long roleId);

    /**
     * Get specific trial record
     * @param roleId Role ID
     * @param trialId Trial ID
     * @return Trial record: trialId, currentStage, bestStage, stars, attempts, etc.
     */
    @GetMapping("/api/trial/{roleId}/{trialId}")
    Map<String, Object> getTrialRecord(@PathVariable("roleId") Long roleId, @PathVariable("trialId") Integer trialId);

    /**
     * Start trial (consume entry)
     * @param roleId Role ID
     * @param trialId Trial ID
     * @return {success, trialRecord{}, remainingAttempts}
     */
    @PostMapping("/api/trial/{roleId}/start/{trialId}")
    Map<String, Object> startTrial(@PathVariable("roleId") Long roleId, @PathVariable("trialId") Integer trialId);

    /**
     * Complete trial (grant rewards)
     * @param roleId Role ID
     * @param trialId Trial ID
     * @param request {score, stars, completionTime}
     * @return {success, rewards[], newBestScore, newBestStage}
     */
    @PostMapping("/api/trial/{roleId}/complete/{trialId}")
    Map<String, Object> completeTrial(
        @PathVariable("roleId") Long roleId, 
        @PathVariable("trialId") Integer trialId,
        @RequestBody Map<String, Object> request
    );

    /**
     * Fail trial (no rewards)
     * @param roleId Role ID
     * @param trialId Trial ID
     * @return {success}
     */
    @PostMapping("/api/trial/{roleId}/fail/{trialId}")
    Map<String, Object> failTrial(@PathVariable("roleId") Long roleId, @PathVariable("trialId") Integer trialId);

    /**
     * Advance to next stage
     * @param roleId Role ID
     * @param trialId Trial ID
     * @return {success, currentStage}
     */
    @PostMapping("/api/trial/{roleId}/advance/{trialId}")
    Map<String, Object> advanceStage(@PathVariable("roleId") Long roleId, @PathVariable("trialId") Integer trialId);

    /**
     * Claim stage reward
     * @param roleId Role ID
     * @param trialId Trial ID
     * @param stageId Stage ID
     * @return {success, rewards[]}
     */
    // Controller returns ResponseEntity<Boolean>
    @PostMapping("/api/trial/{roleId}/claim/{trialId}/{stageId}")
    Boolean claimReward(
        @PathVariable("roleId") Long roleId,
        @PathVariable("trialId") Integer trialId,
        @PathVariable("stageId") Integer stageId
    );

    /**
     * Reset trial progress
     * @param roleId Role ID
     * @param trialId Trial ID
     * @return {success}
     */
    @PostMapping("/api/trial/{roleId}/reset/{trialId}")
    Map<String, Object> resetProgress(@PathVariable("roleId") Long roleId, @PathVariable("trialId") Integer trialId);

    /**
     * Get best score for trial
     * @param roleId Role ID
     * @param trialId Trial ID
     * @return {bestScore, bestStage, bestTime}
     */
    @GetMapping("/api/trial/{roleId}/{trialId}/best")
    Map<String, Object> getBestRecord(@PathVariable("roleId") Long roleId, @PathVariable("trialId") Integer trialId);

    /**
     * Get claimed rewards list
     * @param roleId Role ID
     * @param trialId Trial ID
     * @return {claimedStages[]}
     */
    // Controller returns ResponseEntity<List<Integer>>
    @GetMapping("/api/trial/{roleId}/{trialId}/claimed")
    List<Integer> getClaimedRewards(@PathVariable("roleId") Long roleId, @PathVariable("trialId") Integer trialId);
}
