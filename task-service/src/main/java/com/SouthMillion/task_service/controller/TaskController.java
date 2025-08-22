package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.TaskDomainService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.api.GenericResult;
import org.SouthMillion.dto.task.TaskClaimReq;
import org.SouthMillion.dto.task.TaskListResp;
import org.SouthMillion.dto.task.TaskReportReq;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskDomainService taskService;

    @GetMapping("/{playerId}/all")
    public GenericResult<TaskListResp> all(@PathVariable String playerId) {
        return GenericResult.ok(taskService.getAllTasks(playerId));
    }

    @PostMapping("/report")
    public GenericResult<Void> report(@RequestBody TaskReportReq req) {
        taskService.reportProgress(req);
        return GenericResult.ok(null);
    }

    @PostMapping("/claim")
    public GenericResult<Void> claim(@RequestBody TaskClaimReq req) {
        taskService.claim(req);
        return GenericResult.ok(null);
    }
}