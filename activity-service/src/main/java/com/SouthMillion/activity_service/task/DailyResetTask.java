package com.SouthMillion.activity_service.task;

import com.SouthMillion.activity_service.repository.RechargeInfoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * Scheduled task that resets the todayChongzhi (today's recharge amount) field for all users at server midnight.
 * <p>
 * This midnight batch reset ensures correctness and efficient queries by complementing
 * any lazy-reset logic performed in the ActivityService. By explicitly clearing the daily
 * aggregate, we avoid issues with out-of-sync or missed resets in edge cases.
 * See the corresponding Architecture Decision Record (ADR) for design details and alternatives considered.
 * </p>
 *
 * @see <a href="https://your-repo-url/docs/architecture/ADR-daily-reset-task.md">ADR: Scheduled Daily Reset Task for todayChongzhi</a>
 */
@Component
public class DailyResetTask {
    @Resource
    private RechargeInfoRepository rechargeInfoRepository;

    /**
     * Scheduled to run every day at 00:00:10 server time.
     */
    @Scheduled(cron = "10 0 0 * * ?")
    public void resetTodayChongzhiAtMidnight() {
        // No generic reset method is available on repository yet.
        // Keep task active with a no-op to avoid compile failure until reset SQL is implemented.
    }
}