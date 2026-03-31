package com.SouthMillion.scheduler_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "guild-service", path = "/internal/guild")
public interface GuildServiceClient {

    /** Reset weekly guild activity counters (called by weekly scheduler) */
    @PostMapping("/reset/weekly")
    Map<String, Object> resetWeeklyActivities();
}
