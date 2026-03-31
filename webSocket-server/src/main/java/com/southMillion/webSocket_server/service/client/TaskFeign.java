package com.SouthMillion.webSocket_server.service.client;

import org.SouthMillion.dto.task.TaskListResp;
import org.SouthMillion.dto.task.TaskReportReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Task Service Feign Client
 * 
 * Service: task-service
 * Base Path: /api/task
 * 
 * Endpoints:
 * - GET  /api/task/{roleId}/list         — task list (direct, no GenericResult wrapper)
 * - POST /api/task/advance/{roleId}      — advance to next task, returns new task index
 * - POST /api/task/claim/all/{roleId}    — claim all completed rewards (legacy)
 * - GET  /api/task/progress/{roleId}/{taskKey} — single task progress
 */
@FeignClient(name = "task-service", path = "/api/task")
public interface TaskFeign {

    /**
     * Get full task list for a role.
     * Returns TaskListResp directly (no GenericResult wrapper).
     * Use claimedTasks to determine the current client task_id.
     */
    @GetMapping("/{roleId}/list")
    TaskListResp getTaskList(@PathVariable("roleId") String roleId);

    /**
     * Advance the player to the next task in the tutorial sequence:
     * marks the current un-claimed task as CLAIMED, grants its rewards,
     * and returns the NEW task index (= client task_id to display next).
     */
    @PostMapping("/advance/{roleId}")
    Integer advanceTask(@PathVariable("roleId") String roleId);

    /**
     * Claim all available task rewards for a player (legacy, completion-gated).
     */
    @PostMapping("/claim/all/{roleId}")
    Boolean claimTaskRewards(@PathVariable("roleId") String roleId);

    /**
     * Get specific task progress
     */
    @GetMapping("/progress/{roleId}/{taskKey}")
    Integer getTaskProgress(@PathVariable("roleId") String roleId, @PathVariable("taskKey") String taskKey);

    /**
     * Report raw task progress delta for a player.
     * Used for daily_login and other events initiated inside websocket-server.
     */
    @PostMapping("/report")
    void reportProgress(@RequestBody TaskReportReq req);
}
