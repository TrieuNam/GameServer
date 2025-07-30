package com.SouthMillion.item_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "task-service")
public interface TaskFeignClient {
    @GetMapping("/api/task/completed")
    Boolean hasCompletedTask(@RequestParam("userId") String userId, @RequestParam("taskId") Integer taskId);
}
