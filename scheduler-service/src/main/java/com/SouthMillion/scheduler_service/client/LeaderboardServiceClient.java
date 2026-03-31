package com.SouthMillion.scheduler_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "leaderboard-service", path = "/api/leaderboard")
public interface LeaderboardServiceClient {

    /** Refresh all leaderboard rankings and distribute weekly rewards */
    @PostMapping("/refresh")
    Map<String, Object> refreshAll();
}
