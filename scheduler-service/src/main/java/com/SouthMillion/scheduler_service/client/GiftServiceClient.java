package com.SouthMillion.scheduler_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "gift-service", path = "/internal/gift")
public interface GiftServiceClient {

    /** Send daily login rewards to all eligible players (called by daily scheduler) */
    @PostMapping("/send-daily-rewards")
    Map<String, Object> sendDailyRewards();
}
