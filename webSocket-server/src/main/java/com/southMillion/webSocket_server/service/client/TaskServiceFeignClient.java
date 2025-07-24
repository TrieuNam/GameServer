package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.task.TaskClaimRequest;
import org.SouthMillion.dto.task.TaskProgressDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "task-service")
public interface TaskServiceFeignClient {
    @PostMapping("/task/claim-reward")
    TaskProgressDto claimReward(@RequestBody TaskClaimRequest req);

    @GetMapping("/task/progress")
    List<TaskProgressDto> getTaskProgress(@RequestParam("userId") Long userId);

    @PostMapping("/gm/finish")
    void finishTask(@RequestParam String userId, @RequestParam int taskId);
}