package com.SouthMillion.trial_service.service;

import com.SouthMillion.trial_service.model.entity.TrialRecord;

import java.util.List;

public interface TrialService {
    
    // Basic operations
    List<TrialRecord> getAllRecords(Long userId);
    
    TrialRecord getRecord(Long userId, Integer trialId);
    
    TrialRecord initializeRecord(Long userId, Integer trialId);
    
    // Trial operations
    TrialRecord startTrial(Long userId, Integer trialId);
    
    TrialRecord completeTrial(Long userId, Integer trialId, Long score, Integer stars, Integer completionTime);
    
    TrialRecord failTrial(Long userId, Integer trialId);
    
    TrialRecord advanceStage(Long userId, Integer trialId);
    
    // Reset operations
    void resetDailyAttempts(Long userId, Integer trialId);
    
    void resetAllDailyAttempts(Long userId);
    
    void resetProgress(Long userId, Integer trialId);
    
    // Reward operations
    boolean claimReward(Long userId, Integer trialId, Integer stageId);
    
    boolean isRewardClaimed(Long userId, Integer trialId, Integer stageId);
    
    List<Integer> getClaimedRewards(Long userId, Integer trialId);
    
    // Query operations
    List<TrialRecord> getInProgressTrials(Long userId);
    
    Integer getBestStage(Long userId, Integer trialId);
    
    Long getBestScore(Long userId, Integer trialId);

    Integer getBestTime(Long userId, Integer trialId);
    
    Long getTotalScore(Long userId);
    
    // Validation
    boolean canStartTrial(Long userId, Integer trialId);
    
    boolean hasAttemptsRemaining(Long userId, Integer trialId);
}
