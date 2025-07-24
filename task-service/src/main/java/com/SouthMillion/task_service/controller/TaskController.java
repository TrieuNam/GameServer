package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.task.TaskClaimRequest;
import org.SouthMillion.dto.task.TaskProgressDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    // 1452: Lấy tiến độ nhiệm vụ
    @GetMapping("/progress")
    public List<TaskProgressDto> getTaskProgress(@RequestParam Long userId) {
        return taskService.getTaskProgress(userId);
    }

    // 1451: Nhận thưởng nhiệm vụ
    @PostMapping("/claim-reward")
    public TaskProgressDto claimReward(@RequestBody TaskClaimRequest req) {
        return taskService.claimTask(req);
    }
}