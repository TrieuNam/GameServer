package com.SouthMillion.scheduler_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "rune-service", path = "/internal/rune")
public interface RuneServiceClient {

    /**
     * Reset daily_reward flag to 0 for all players.
     * Called every day at midnight by DailyResetJob.
     */
    @PostMapping("/reset/daily")
    Map<String, Object> resetDailyReward();
}
