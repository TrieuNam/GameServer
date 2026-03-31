package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.trial.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC Client for trial-service
 * Handles trial/challenge tower operations
 * Target: <15ms per RPC call
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialGrpcClient {

    @GrpcClient("trial-service")
    private TrialServiceGrpc.TrialServiceBlockingStub trialServiceStub;

    /**
     * Get single trial record
     */
    public Map<String, Object> getTrialRecord(Long roleId, Integer trialId) {
        try {
            log.debug("[grpc-trial] GetTrialRecord: roleId={}, trialId={}", roleId, trialId);
            
            GetTrialRecordRequest request = GetTrialRecordRequest.newBuilder()
                    .setUserId(roleId)
                    .setTrialId(trialId)
                    .build();
            
            TrialRecordResponse response = trialServiceStub.getTrialRecord(request);
            
            if (response.getStatus().getSuccess()) {
                return convertRecordToMap(response.getRecord());
            } else {
                log.error("[grpc-trial] GetTrialRecord failed: {}", response.getStatus().getMessage());
                return null;
            }
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to get trial record: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get all trial records for role
     */
    public List<Map<String, Object>> getRoleTrials(Long roleId) {
        try {
            log.debug("[grpc-trial] GetAllTrialRecords: roleId={}", roleId);
            
            GetAllTrialRecordsRequest request = GetAllTrialRecordsRequest.newBuilder()
                    .setUserId(roleId)
                    .build();
            
            AllTrialRecordsResponse response = trialServiceStub.getAllTrialRecords(request);
            
            if (response.getStatus().getSuccess()) {
                return response.getRecordsList().stream()
                        .map(this::convertRecordToMap)
                        .collect(Collectors.toList());
            } else {
                log.error("[grpc-trial] GetAllTrialRecords failed: {}", response.getStatus().getMessage());
                return Collections.emptyList();
            }
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to get trials: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Start trial
     */
    public Map<String, Object> startTrial(Long roleId, Integer trialId) {
        try {
            log.info("[grpc-trial] StartTrial: roleId={}, trialId={}", roleId, trialId);
            
            StartTrialRequest request = StartTrialRequest.newBuilder()
                    .setUserId(roleId)
                    .setTrialId(trialId)
                    .build();
            
            TrialRecordResponse response = trialServiceStub.startTrial(request);
            
            if (response.getStatus().getSuccess()) {
                return convertRecordToMap(response.getRecord());
            } else {
                log.error("[grpc-trial] StartTrial failed: {}", response.getStatus().getMessage());
                return null;
            }
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to start trial: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Complete trial
     */
    public Map<String, Object> completeTrial(Long roleId, Integer trialId, Long score, Integer stars, Integer completionTime) {
        try {
            log.info("[grpc-trial] CompleteTrial: roleId={}, trialId={}, score={}, stars={}", 
                roleId, trialId, score, stars);
            
            CompleteTrialRequest request = CompleteTrialRequest.newBuilder()
                    .setUserId(roleId)
                    .setTrialId(trialId)
                    .setScore(score != null ? score : 0)
                    .setStars(stars != null ? stars : 0)
                    .setCompletionTime(completionTime != null ? completionTime : 0)
                    .build();
            
            TrialRecordResponse response = trialServiceStub.completeTrial(request);
            
            if (response.getStatus().getSuccess()) {
                return convertRecordToMap(response.getRecord());
            } else {
                log.error("[grpc-trial] CompleteTrial failed: {}", response.getStatus().getMessage());
                return null;
            }
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to complete trial: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fail trial
     */
    public Map<String, Object> failTrial(Long roleId, Integer trialId) {
        try {
            log.info("[grpc-trial] FailTrial: roleId={}, trialId={}", roleId, trialId);
            
            FailTrialRequest request = FailTrialRequest.newBuilder()
                    .setUserId(roleId)
                    .setTrialId(trialId)
                    .build();
            
            TrialRecordResponse response = trialServiceStub.failTrial(request);
            
            if (response.getStatus().getSuccess()) {
                return convertRecordToMap(response.getRecord());
            } else {
                log.error("[grpc-trial] FailTrial failed: {}", response.getStatus().getMessage());
                return null;
            }
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to fail trial: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Advance trial stage
     */
    public Map<String, Object> advanceStage(Long roleId, Integer trialId) {
        try {
            log.info("[grpc-trial] AdvanceStage: roleId={}, trialId={}", roleId, trialId);
            
            AdvanceStageRequest request = AdvanceStageRequest.newBuilder()
                    .setUserId(roleId)
                    .setTrialId(trialId)
                    .build();
            
            TrialRecordResponse response = trialServiceStub.advanceStage(request);
            
            if (response.getStatus().getSuccess()) {
                return convertRecordToMap(response.getRecord());
            } else {
                log.error("[grpc-trial] AdvanceStage failed: {}", response.getStatus().getMessage());
                return null;
            }
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to advance stage: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Claim stage reward
     */
    public Map<String, Object> claimReward(Long roleId, Integer trialId, Integer stageId) {
        try {
            log.info("[grpc-trial] ClaimReward: roleId={}, trialId={}, stageId={}", 
                roleId, trialId, stageId);
            
            ClaimRewardRequest request = ClaimRewardRequest.newBuilder()
                    .setUserId(roleId)
                    .setTrialId(trialId)
                    .setStageId(stageId)
                    .build();
            
            ClaimRewardResponse response = trialServiceStub.claimReward(request);
            
            if (response.getStatus().getSuccess()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", response.getSuccess());
                
                // Rewards
                if (response.hasRewards()) {
                    Map<String, Object> rewards = new HashMap<>();
                    rewards.put("exp", response.getRewards().getExp());
                    
                    // Currencies (gold, diamond, etc.)
                    Map<String, Long> currencies = new HashMap<>();
                    response.getRewards().getCurrenciesList().forEach(currency -> {
                        currencies.put(currency.getCurrencyType(), currency.getAmount());
                    });
                    rewards.put("currencies", currencies);
                    
                    // Items
                    List<Map<String, Object>> items = response.getRewards().getItemsList().stream()
                            .map(item -> {
                                Map<String, Object> itemMap = new HashMap<>();
                                itemMap.put("itemId", item.getItemId());
                                itemMap.put("quantity", item.getQuantity());
                                return itemMap;
                            })
                            .collect(Collectors.toList());
                    rewards.put("items", items);
                    
                    result.put("rewards", rewards);
                }
                
                return result;
            } else {
                log.error("[grpc-trial] ClaimReward failed: {}", response.getStatus().getMessage());
                return null;
            }
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to claim reward: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Reset daily attempts
     */
    public boolean resetDailyAttempts(Long roleId, Integer trialId) {
        try {
            log.info("[grpc-trial] ResetDailyAttempts: roleId={}, trialId={}", roleId, trialId);
            
            ResetDailyAttemptsRequest request = ResetDailyAttemptsRequest.newBuilder()
                    .setUserId(roleId)
                    .setTrialId(trialId != null ? trialId : 0) // 0 = reset all
                    .build();
            
            org.SouthMillion.grpc.common.ResponseStatus response = trialServiceStub.resetDailyAttempts(request);
            
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            log.error("[grpc-trial] Failed to reset daily attempts: {}", e.getMessage());
            return false;
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert proto TrialRecord to Map
     */
    private Map<String, Object> convertRecordToMap(org.SouthMillion.grpc.trial.TrialRecord record) {
        Map<String, Object> map = new HashMap<>();
        
        map.put("userId", record.getUserId());
        map.put("trialId", record.getTrialId());
        map.put("currentStage", record.getCurrentStage());
        map.put("bestStage", record.getBestStage());
        map.put("bestScore", record.getBestScore());
        map.put("totalAttempts", record.getTotalAttempts());
        map.put("dailyAttempts", record.getDailyAttempts());
        map.put("maxDailyAttempts", record.getMaxDailyAttempts());
        map.put("status", record.getStatus());
        map.put("lastAttemptTime", record.getLastAttemptTime());
        map.put("createdAt", record.getCreatedAt());
        map.put("updatedAt", record.getUpdatedAt());
        map.put("claimedRewardStages", record.getClaimedRewardStagesList());
        map.put("starsEarned", record.getStarsEarned());
        
        return map;
    }
}
