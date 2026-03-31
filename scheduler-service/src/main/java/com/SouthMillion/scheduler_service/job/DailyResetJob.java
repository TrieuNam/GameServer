package com.SouthMillion.scheduler_service.job;

import com.SouthMillion.scheduler_service.client.GiftServiceClient;
import com.SouthMillion.scheduler_service.client.ShopServiceClient;
import com.SouthMillion.scheduler_service.client.TaskServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyResetJob {

    private final TaskServiceClient taskServiceClient;
    private final ShopServiceClient shopServiceClient;
    private final GiftServiceClient giftServiceClient;

    @Scheduled(cron = "${scheduler.daily-reset-cron:0 0 0 * * ?}")
    public void executeDailyReset() {
        log.info("Starting daily reset at {}", LocalDateTime.now());

        resetDailyTasks();
        resetDailyShop();
        resetArenaAttempts();
        distributeDailyRewards();

        log.info("Daily reset completed at {}", LocalDateTime.now());
    }

    private void resetDailyTasks() {
        try {
            taskServiceClient.resetDailyTasks();
            log.info("[DailyReset] Daily tasks reset successfully");
        } catch (Exception e) {
            log.error("[DailyReset] Failed to reset daily tasks: {}", e.getMessage());
        }
    }

    private void resetDailyShop() {
        try {
            shopServiceClient.resetDailyItems();
            log.info("[DailyReset] Daily shop refreshed successfully");
        } catch (Exception e) {
            log.error("[DailyReset] Failed to refresh daily shop: {}", e.getMessage());
        }
    }

    private void resetArenaAttempts() {
        // Arena challenge counts reset lazily on first access each day (date-based check in ArenaService).
        // No explicit call needed — arena-service auto-resets when getChallengesRemaining() is called.
        log.info("[DailyReset] Arena attempts reset deferred to lazy evaluation in arena-service");
    }

    private void distributeDailyRewards() {
        try {
            giftServiceClient.sendDailyRewards();
            log.info("[DailyReset] Daily rewards distributed successfully");
        } catch (Exception e) {
            log.error("[DailyReset] Failed to distribute daily rewards: {}", e.getMessage());
        }
    }
}
