package com.SouthMillion.escort_service.service;

import com.SouthMillion.escort_service.model.entity.EscortMission;
import com.SouthMillion.escort_service.model.entity.EscortStats;

import java.util.List;

public interface EscortService {
    
    // Mission operations
    List<EscortMission> getAllMissions(String userId);
    
    EscortMission getMission(String userId, Long missionId);
    
    EscortMission generateMission(String userId, Integer quality);
    
    EscortMission startMission(String userId, Long missionId);
    
    EscortMission updateProgress(String userId, Long missionId, Integer progressIncrement);
    
    EscortMission completeMission(String userId, Long missionId);
    
    EscortMission failMission(String userId, Long missionId);
    
    void cancelMission(String userId, Long missionId);
    
    EscortMission claimReward(String userId, Long missionId);
    
    // Mission management
    List<EscortMission> refreshMissions(String userId);
    
    void checkExpiredMissions(String userId);
    
    List<EscortMission> getActiveMissions(String userId);
    
    List<EscortMission> getCompletedMissions(String userId);
    
    List<EscortMission> getUnclaimedRewards(String userId);
    
    // Statistics operations
    EscortStats getStats(String userId);
    
    EscortStats initializeStats(String userId);

    /** Persist stats changes (intercept/help counters updated externally) */
    EscortStats saveStats(EscortStats stats);

    /** Record an intercept action and persist immediately */
    EscortStats recordIntercept(String userId);

    /** Record a help action and persist immediately */
    EscortStats recordHelp(String userId);

    /**
     * Auto-complete all IN_PROGRESS missions for a user that have reached maximum
     * progress (or set progress = distance so they can be claimed).
     * Called before claimReward to handle the case where the client never sends
     * a separate "mission complete" request.
     */
    void autoCompleteMissions(String userId);

    void resetDailyStats(String userId);
    
    // Validation
    boolean canStartMission(String userId);
    
    boolean canRefresh(String userId);
    
    boolean hasActiveMission(String userId);

    /**
     * Rob/intercept another player's active escort mission.
     * Finds the victim's first IN_PROGRESS mission and marks it FAILED.
     *
     * @return the intercepted mission, or null if victim has no active mission
     */
    EscortMission robEscort(String userId, String victimId);

    /**
     * Speed up the user's active escort mission (consume item on client side).
     * Sets mission progress = distance so it can be completed immediately.
     *
     * @param missionId the mission to speed up
     * @return the updated mission
     */
    EscortMission speedupEscort(String userId, Long missionId);
}
