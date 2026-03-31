package com.SouthMillion.scheduler_service.job;

import com.SouthMillion.scheduler_service.client.GuildServiceClient;
import com.SouthMillion.scheduler_service.client.LeaderboardServiceClient;
import com.SouthMillion.scheduler_service.client.TaskServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyResetJob {

    private final TaskServiceClient taskServiceClient;
    private final GuildServiceClient guildServiceClient;
    private final LeaderboardServiceClient leaderboardServiceClient;

    @Scheduled(cron = "${scheduler.weekly-reset-cron:0 0 0 ? * MON}")
    public void executeWeeklyReset() {
        log.info("Starting weekly reset at {}", LocalDateTime.now());

        resetWeeklyTasks();
        resetGuildActivities();
        distributeWeeklyRewards();

        log.info("Weekly reset completed at {}", LocalDateTime.now());
    }

    private void resetWeeklyTasks() {
        try {
            taskServiceClient.resetWeeklyTasks();
            log.info("[WeeklyReset] Weekly tasks reset successfully");
        } catch (Exception e) {
            log.error("[WeeklyReset] Failed to reset weekly tasks: {}", e.getMessage());
        }
    }

    private void resetGuildActivities() {
        try {
            guildServiceClient.resetWeeklyActivities();
            log.info("[WeeklyReset] Guild weekly activities reset successfully");
        } catch (Exception e) {
            log.error("[WeeklyReset] Failed to reset guild activities: {}", e.getMessage());
        }
    }

    private void distributeWeeklyRewards() {
        try {
            leaderboardServiceClient.refreshAll();
            log.info("[WeeklyReset] Leaderboard refreshed and weekly rewards distributed");
        } catch (Exception e) {
            log.error("[WeeklyReset] Failed to distribute weekly rewards: {}", e.getMessage());
        }
    }
}
