package com.SouthMillion.escort_service.scheduler;

import com.SouthMillion.escort_service.repository.EscortMissionRepository;
import com.SouthMillion.escort_service.service.EscortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Escort Mission Scheduler
 *
 * Runs periodically to auto-complete escort missions that are still IN_PROGRESS
 * without the client explicitly sending a "complete" request.
 *
 * This addresses the case raised in Session 10 Next Steps:
 *   "Scheduler auto-complete escort missions theo timer
 *    (hiện autoCompleteMissions chỉ chạy on-demand)"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscortScheduler {

    private static final int STATUS_IN_PROGRESS = 1;

    private final EscortMissionRepository missionRepository;
    private final EscortService escortService;

    /**
     * Every 5 minutes, find all users who have IN_PROGRESS missions and call
     * {@link EscortService#autoCompleteMissions(String)} for each.
     *
     * The autoCompleteMissions() method (added in Session 10) sets progress=distance
     * for each mission and calls completeMission(), making them claimable.
     *
     * Fixed rate of 300 000 ms (5 min) with an initial delay of 60 000 ms (1 min)
     * so the service has time to start before the first run.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    public void autoCompleteInProgressMissions() {
        try {
            List<Long> activeUserIds = missionRepository.findDistinctUserIdsByStatus(STATUS_IN_PROGRESS);
            if (activeUserIds.isEmpty()) {
                return;
            }
            log.info("[EscortScheduler] Auto-completing missions for {} active users", activeUserIds.size());
            int completed = 0;
            for (Long userId : activeUserIds) {
                try {
                    escortService.autoCompleteMissions(userId.toString());
                    completed++;
                } catch (Exception ex) {
                    log.warn("[EscortScheduler] Failed to auto-complete missions for userId={}: {}",
                            userId, ex.getMessage());
                }
            }
            log.info("[EscortScheduler] Done. Processed {}/{} users", completed, activeUserIds.size());
        } catch (Exception ex) {
            log.error("[EscortScheduler] Unexpected error during auto-complete run", ex);
        }
    }
}
