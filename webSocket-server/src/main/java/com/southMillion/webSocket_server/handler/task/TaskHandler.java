package com.SouthMillion.webSocket_server.handler.task;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskConditionRegistry;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.TaskFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.task.TaskDTO;
import org.SouthMillion.dto.task.TaskListResp;
import org.SouthMillion.dto.task.TaskStatus;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task Handler — Handles the main tutorial / quest chain.
 *
 * Proto messages (msgrole.proto):
 *   1451  PB_CSFetchTaskRewardReq  — client wants to claim current task reward
 *   1452  PB_SCTaskProgressInfo    — server pushes {id, progress} of the current task
 *
 * Legacy task mapping:
 *   The original C++ server pushes the real numeric task_id of the current task.
 *   For imported legacy configs we therefore derive the first non-claimed task and
 *   send its numeric key when possible instead of just using claimedTasks.
 *
 * Progress value rule:
 *   - status == COMPLETED  → send targetValue  (claim button becomes active on client)
 *   - status == IN_PROGRESS / NOT_STARTED → send real currentProgress (0 = not done)
 *   This prevents the client from showing a "ready to claim" button for tasks that
 *   have not been completed yet (e.g. join_guild while guild-service is down).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskHandler implements MessageHandler {

    private final TaskFeign taskFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final ConcurrentHashMap<Long, TaskProgressSnapshot> lastKnownProgress = new ConcurrentHashMap<>();

    private static final int MSGID_CLAIM_TASK_REWARD_REQ = 1451;  // PB_CSFetchTaskRewardReq
    private static final int MSGID_TASK_PROGRESS_RESP    = 1452;  // PB_SCTaskProgressInfo

    @Override
    public int[] interests() {
        return new int[]{MSGID_CLAIM_TASK_REWARD_REQ};
    }

    // ─── Bootstrap ─────────────────────────────────────────────────────────────

    /** Called after login: push the player's current task state (msgId 1452). */
    public Mono<Void> pushAll(PlayerSession session) {
        return sendAllTaskProgress(session);
    }

    /**
     * Report a daily login event to task-service so the daily_login task
     * progresses automatically on every login session.
     * Returns Mono<Void> for use in the bootstrap Mono.when() chain.
     */
    public Mono<Void> reportDailyLogin(PlayerSession session) {
        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
            if (roleId == null) return;
            try {
                taskProgressPublisher.publish(roleId, "daily_login", 1, "websocket-login-bootstrap");
                log.debug("[Task] Reported daily_login for roleId={}", roleId);
            } catch (Exception e) {
                log.warn("[Task] Failed to report daily_login for roleId={}: {}", roleId, e.getMessage());
            }
        });
    }

    /**
     * Query the task-service for the player's real task state and push it.
     * Uses actual status/progress so the client only shows the claim button
     * when the task is genuinely COMPLETED.
     */
    public Mono<Void> sendAllTaskProgress(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();

        return Mono.fromRunnable(() -> pushCurrentTaskProgress(session));
    }

    // ─── Message handler ───────────────────────────────────────────────────────

    /**
     * Handle PB_CSFetchTaskRewardReq (1451):
     * 1. Call advanceTask() on task-service — marks current task CLAIMED only if it is COMPLETED.
     * 2. Push the current task's real state (id + progress) after the advance attempt.
     *    If the task was not completed, advanceTask() returns the same index and
     *    pushCurrentTaskProgress() will send the real (non-complete) progress —
     *    so the client claim button disappears correctly.
     */
    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        if (msgId != MSGID_CLAIM_TASK_REWARD_REQ) return Mono.empty();

        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
            if (roleId == null) {
                log.warn("[Task] claim rejected — roleId is null");
                return;
            }

            try {
                // Parse request (empty proto, just for validation)
                Msgrole.PB_CSFetchTaskRewardReq.parseFrom(
                        payload == null ? new byte[0] : payload);
            } catch (Exception e) {
                log.warn("[Task] Failed to parse PB_CSFetchTaskRewardReq: {}", e.toString());
            }

            try {
                log.info("[Task] Claim task reward — roleId={}", roleId);

                // Advance to next task (claim current only if it is COMPLETED + grant rewards).
                // If task is NOT COMPLETED, advanceTask returns the same index without changing state.
                Integer newTaskId = taskFeign.advanceTask(String.valueOf(roleId));
                log.info("[Task] advanceTask returned newTaskId={} for roleId={}", newTaskId, roleId);

            } catch (Exception e) {
                log.error("[Task] advanceTask failed — roleId={}: {}", roleId, e.toString());
            }

            // Always push the real current task state after the claim attempt.
            // This correctly shows: claim succeeded → next task; claim blocked → same task at real progress.
            pushCurrentTaskProgress(session);
        });
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Query task-service and push the correct progress for the player's current task.
     *
     * Progress rules (prevents false "task complete" on client):
     *   COMPLETED    → targetValue   (claim button visible)
     *   IN_PROGRESS  → currentProgress (real value, < targetValue)
     *   NOT_STARTED  → 0
     *   All tasks done / task-service down → fallback progress=0
     */
    public void pushCurrentTaskProgress(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return;

        int taskId = 0;
        int progress = 0;

        try {
            TaskListResp resp = taskFeign.getTaskList(String.valueOf(roleId));

            if (resp != null) {
                taskId = resp.getClaimedTasks() != null ? resp.getClaimedTasks() : 0;
                List<TaskDTO> tasks = resp.getTasks();

                if (tasks != null) {
                    int currentIndex = findCurrentTaskIndex(tasks);
                    if (currentIndex >= 0 && currentIndex < tasks.size()) {
                        TaskDTO current = tasks.get(currentIndex);
                        taskId = parseLegacyTaskId(current.getTaskKey()).orElse(currentIndex);
                        TaskStatus status = current.getStatus();

                        if (status == TaskStatus.COMPLETED) {
                            progress = current.getTargetValue() != null ? current.getTargetValue() : 1;
                        } else {
                            progress = current.getCurrentProgress() != null ? current.getCurrentProgress() : 0;
                        }
                    }
                }
            }

            log.info("[Task] Pushed task state — roleId={} taskId={} progress={} (status check enforced)",
                    roleId, taskId, progress);
            lastKnownProgress.put(roleId, new TaskProgressSnapshot(taskId, progress));
        } catch (Exception e) {
            TaskProgressSnapshot snap = lastKnownProgress.get(roleId);
            if (snap != null) {
                taskId = snap.taskId();
                progress = snap.progress();
                log.warn("[Task] Failed to get task list for roleId={}, fallback to last-known id={} progress={}: {}",
                        roleId, taskId, progress, e.getMessage());
            } else {
                log.warn("[Task] Failed to get task list for roleId={}, fallback progress=0: {}", roleId, e.getMessage());
            }
        }

        sendProgress(session, taskId, progress);
    }

    public void pushCurrentTaskProgressIfRelevant(PlayerSession session, String reportedTaskKey) {
        Long roleId = session.getRoleId();
        if (roleId == null || reportedTaskKey == null || reportedTaskKey.isBlank()) {
            return;
        }
        log.debug("[TaskIfRelevant] roleId={} reportedTaskKey={} → evaluating match", roleId, reportedTaskKey);

        try {
            TaskListResp resp = taskFeign.getTaskList(String.valueOf(roleId));
            if (!matchesCurrentTask(resp, reportedTaskKey)) {
                log.debug("[TaskIfRelevant] roleId={} reportedTaskKey={} → SKIPPED (current task mismatch)",
                        roleId, reportedTaskKey);
                return;
            }
            log.debug("[TaskIfRelevant] roleId={} reportedTaskKey={} → MATCHED, pushing progress", roleId, reportedTaskKey);
        } catch (Exception e) {
            log.debug("[TaskIfRelevant] roleId={} reportedTaskKey={} → SKIPPED (getTaskList failed: {})",
                    roleId, reportedTaskKey, e.toString());
            return;
        }

        pushCurrentTaskProgress(session);
    }

    /** Send PB_SCTaskProgressInfo (1452) to client. */
    private void sendProgress(PlayerSession session, int taskId, int progress) {
        try {
            Msgrole.PB_SCTaskProgressInfo info = Msgrole.PB_SCTaskProgressInfo.newBuilder()
                    .setId(taskId)
                    .setProgress(progress)
                    .build();
            Emitters.emit(session, MSGID_TASK_PROGRESS_RESP, info.toByteArray());
            log.debug("[Task] Sent 1452 id={} progress={} → roleId={}", taskId, progress, session.getRoleId());
        } catch (Exception e) {
            log.error("[Task] Failed to send 1452 — roleId={}: {}", session.getRoleId(), e.toString());
        }
    }

    private int findCurrentTaskIndex(List<TaskDTO> tasks) {
        for (int index = 0; index < tasks.size(); index++) {
            TaskStatus status = tasks.get(index).getStatus();
            if (status != TaskStatus.CLAIMED) {
                return index;
            }
        }
        return -1;
    }

    private OptionalInt parseLegacyTaskId(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(taskKey));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    private boolean matchesCurrentTask(TaskListResp resp, String reportedTaskKey) {
        if (resp == null || resp.getTasks() == null || reportedTaskKey == null || reportedTaskKey.isBlank()) {
            log.debug("[TaskMatch] reportedKey={} → resp null or no tasks", reportedTaskKey);
            return false;
        }

        int currentIndex = findCurrentTaskIndex(resp.getTasks());
        if (currentIndex < 0 || currentIndex >= resp.getTasks().size()) {
            log.debug("[TaskMatch] reportedKey={} → no active task (all claimed or empty list)", reportedTaskKey);
            return false;
        }

        TaskDTO currentTask = resp.getTasks().get(currentIndex);
        String currentTaskKey = currentTask.getTaskKey();
        if (currentTaskKey == null || currentTaskKey.isBlank()) {
            log.debug("[TaskMatch] reportedKey={} → current task at index={} has blank key", reportedTaskKey, currentIndex);
            return false;
        }

        if (currentTaskKey.equals(reportedTaskKey)) {
            log.debug("[TaskMatch] reportedKey={} → exact match with currentTask={}", reportedTaskKey, currentTaskKey);
            return true;
        }

        Integer currentConditionType = currentTask.getLegacyConditionType();
        if (currentConditionType == null) {
            currentConditionType = TaskConditionRegistry.resolveConditionType(currentTaskKey);
        }
        Integer reportedConditionType = TaskConditionRegistry.resolveConditionType(reportedTaskKey);
        boolean matched = currentConditionType != null && currentConditionType.equals(reportedConditionType);
        log.debug("[TaskMatch] reportedKey={} reportedCondition={} | currentTask={} currentCondition={} → {}",
            reportedTaskKey, reportedConditionType, currentTaskKey, currentConditionType, matched ? "MATCH" : "MISMATCH");
        return matched;
    }

    private record TaskProgressSnapshot(int taskId, int progress) {}
}
