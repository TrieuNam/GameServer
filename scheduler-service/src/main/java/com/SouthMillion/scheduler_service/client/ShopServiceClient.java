package com.SouthMillion.scheduler_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "shop-service", path = "/internal/shop")
public interface ShopServiceClient {

    /** Refresh all daily shop items for all players (called by daily scheduler) */
    @PostMapping("/reset/daily")
    Map<String, Object> resetDailyItems();
}
