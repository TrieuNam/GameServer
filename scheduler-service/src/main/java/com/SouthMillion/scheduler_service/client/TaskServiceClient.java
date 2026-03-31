package com.SouthMillion.scheduler_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "task-service", path = "/internal/task")
public interface TaskServiceClient {

    /** Reset all players' daily task progress (called by daily scheduler) */
    @PostMapping("/reset/daily")
    Map<String, Object> resetDailyTasks();

    /** Reset all players' weekly task progress (called by weekly scheduler) */
    @PostMapping("/reset/weekly")
    Map<String, Object> resetWeeklyTasks();
}
