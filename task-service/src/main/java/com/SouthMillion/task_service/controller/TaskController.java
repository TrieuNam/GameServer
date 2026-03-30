package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.TaskDomainService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.api.GenericResult;
import org.SouthMillion.dto.task.TaskClaimReq;
import org.SouthMillion.dto.task.TaskListResp;
import org.SouthMillion.dto.task.TaskReportReq;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /**
     * Get task list for role — returns TaskListResp directly (no GenericResult wrapper).
     * Called by WebSocket TaskHandler via TaskFeign.
     */
    @GetMapping("/{roleId}/list")
    public TaskListResp list(@PathVariable String roleId) {
        return taskService.getAllTasks(roleId);
    }

    /**
     * Advance player to next task: marks the current (un-claimed) task as CLAIMED,
     * grants rewards, and returns the new task index (= new client task_id).
     * Called by WebSocket TaskHandler via TaskFeign on PB_CSFetchTaskRewardReq (1451).
     */
    @PostMapping("/advance/{roleId}")
    public Integer advance(@PathVariable String roleId) {
        return taskService.advanceTask(roleId);
    }

    /**
     * Claim all completed task rewards for a player
     * Called by WebSocket TaskHandler via TaskFeign
     * @param roleId Role ID
     * @return true if any tasks were claimed
     */
    @PostMapping("/claim/all/{roleId}")
    public Boolean claimAll(@PathVariable String roleId) {
        taskService.claimAllCompletedTasks(roleId);
        return true;
    }

    /**
     * Get specific task progress
     * Called by WebSocket TaskHandler via TaskFeign
     * @param roleId Role ID
     * @param taskKey Task key string
     * @return Current progress value
     */
    @GetMapping("/progress/{roleId}/{taskKey}")
    public Integer getProgress(@PathVariable String roleId, @PathVariable String taskKey) {
        return taskService.getTaskProgress(roleId, taskKey);
    }

    @GetMapping("/status/{roleId}/{taskKey}")
    public Map<String, Object> getStatus(@PathVariable String roleId, @PathVariable String taskKey) {
        return taskService.getTaskState(roleId, taskKey);
    }
}