package com.SouthMillion.webSocket_server.handler.task;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.LoginSnapshotService;
import com.SouthMillion.webSocket_server.service.TaskCondition;
import com.SouthMillion.webSocket_server.service.TaskConditionRegistry;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.TaskFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.task.TaskDTO;
import org.SouthMillion.dto.task.TaskListResp;
import org.SouthMillion.dto.task.TaskStatus;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;
    private final LoginSnapshotService loginSnapshotService;
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

            int claimedBefore = fetchClaimedTaskCount(roleId);
            try {
                log.info("[Task] Claim task reward — roleId={}", roleId);

                // Advance to next task (claim current only if it is COMPLETED + grant rewards).
                // If task is NOT COMPLETED, advanceTask returns the same index without changing state.
                Integer newTaskId = taskFeign.advanceTask(String.valueOf(roleId));
                int claimedAfter = fetchClaimedTaskCount(roleId);
                boolean rewardClaimed = claimedAfter > claimedBefore;
                log.info("[Task] advanceTask returned newTaskId={} for roleId={} (claimedBefore={} claimedAfter={} rewardClaimed={})",
                        newTaskId, roleId, claimedBefore, claimedAfter, rewardClaimed);
                if (rewardClaimed) {
                    syncPostClaimState(session, roleId);
                }

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
     *   condition 1 / level reach → binary 0 or 1 (avoid misleading 0/3, 0/10, ...)
     *   other COMPLETED tasks     → targetValue   (claim button visible)
     *   other IN_PROGRESS tasks   → currentProgress (real value, < targetValue)
     *   NOT_STARTED               → 0
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
                        progress = toClientProgress(current);
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

    /**
     * Report a level-up event to task-service and push the updated progress to the client.
     * Uses SNAPSHOT_MAX semantics (task-service stores the max level seen). For legacy
     * condition 1 tasks the client now shows a binary 0/1 bar, so we skip the old per-level
     * sweep and push the authoritative state instead of a misleading 0/3, 0/10, ... animation.
     *
     * @param session   player session
     * @param oldLevel  level before the change
     * @param newLevel  level after the change
     */
    public void pushLevelUpProgress(PlayerSession session, int oldLevel, int newLevel) {
        Long roleId = session.getRoleId();
        if (roleId == null || newLevel <= oldLevel) return;

        // Report final level to task-service (SNAPSHOT_MAX: stores max(current, newLevel))
        taskProgressPublisher.publish(roleId, TaskCondition.LEVEL_UP.taskKey(), newLevel, "role-level-change");

        // Resolve current task so we can send intermediate 1452 packets for bar animation
        try {
            TaskListResp resp = taskFeign.getTaskList(String.valueOf(roleId));
            if (resp == null || resp.getTasks() == null) {
                pushCurrentTaskProgress(session);
                return;
            }
            int currentIndex = findCurrentTaskIndex(resp.getTasks());
            if (currentIndex < 0 || currentIndex >= resp.getTasks().size()) {
                pushCurrentTaskProgress(session);
                return;
            }
            TaskDTO current = resp.getTasks().get(currentIndex);

            // Only animate if the current task is a level_up condition.
            // Legacy condition 1 is now displayed as a binary 0/1 bar on the client,
            // so we skip the old per-level sweep and just push the authoritative state.
            TaskCondition cond = TaskCondition.fromTaskKey(current.getTaskKey());
            if (cond != TaskCondition.LEVEL_UP || usesBinaryClientProgress(current)) {
                pushCurrentTaskProgress(session);
                return;
            }

            int taskId = parseLegacyTaskId(current.getTaskKey()).orElse(currentIndex);
            int target = current.getTargetValue() != null ? current.getTargetValue() : newLevel;

            // Send one 1452 per intermediate level so the client bar sweeps smoothly.
            // Cap at 10 intermediate steps to avoid flooding the socket on large level gaps.
            int steps = Math.min(newLevel - oldLevel - 1, 10);
            if (steps > 0) {
                double step = (double)(newLevel - oldLevel) / (steps + 1);
                for (int i = 1; i <= steps; i++) {
                    int midLevel = oldLevel + (int) Math.round(step * i);
                    if (midLevel >= target) break;
                    sendProgress(session, taskId, midLevel);
                }
            }
        } catch (Exception e) {
            log.debug("[Task] pushLevelUpProgress animation skipped roleId={} ex={}", roleId, e.toString());
        }

        // Push the authoritative final state
        pushCurrentTaskProgress(session);
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

    private int fetchClaimedTaskCount(Long roleId) {
        if (roleId == null) {
            return 0;
        }
        try {
            TaskListResp resp = taskFeign.getTaskList(String.valueOf(roleId));
            return resp != null && resp.getClaimedTasks() != null ? resp.getClaimedTasks() : 0;
        } catch (Exception e) {
            log.debug("[Task] fetchClaimedTaskCount fallback roleId={} ex={}", roleId, e.toString());
            return 0;
        }
    }

    private void syncPostClaimState(PlayerSession session, Long roleId) {
        // Wallet is updated synchronously by task-service, so push it right away.
        pushWalletBalance(session, roleId);

        // Bag rewards can still race with the async grant flow. Try one immediate full-bag push first.
        // If that first push succeeds, do not schedule a blind second UI refresh.
        // If it fails, keep the fallback retry so the later update can still recover the UI state.
        boolean immediateBagPushSucceeded = pushBagState(session, roleId);
        if (immediateBagPushSucceeded) {
            return;
        }

        Mono.delay(Duration.ofMillis(1500))
                .onErrorResume(ex -> Mono.empty())
                .subscribe(ignored -> {
                    pushBagState(session, roleId);
                    pushWalletBalance(session, roleId);
                });
    }

    private boolean pushBagState(PlayerSession session, Long roleId) {
        if (roleId == null) {
            return false;
        }
        try {
            List<BagDTOs.ItemView> list = bagFeign.list(String.valueOf(roleId));
            Emitters.sendKnapsackAllInfo(session, list);
            return true;
        } catch (Exception e) {
            log.warn("[Task] Failed to push bag state for roleId={}: {}", roleId, e.getMessage());
            return false;
        }
    }

    private void pushWalletBalance(PlayerSession session, Long roleId) {
        if (roleId == null) {
            return;
        }
        try {
            WalletDTOs.BalancesResp walletResp = walletHttpClient.info(String.valueOf(roleId));
            if (walletResp != null && walletResp.balances() != null) {
                Emitters.sendWalletBalances(session, walletResp.balances());
            }
        } catch (Exception e) {
            log.warn("[Task] Failed to push wallet balance for roleId={}: {}", roleId, e.getMessage());
        }
    }

    int toClientProgress(TaskDTO current) {
        if (current == null) {
            return 0;
        }

        int target = current.getTargetValue() != null ? Math.max(current.getTargetValue(), 1) : 1;
        int currentProgress = current.getCurrentProgress() != null ? Math.max(current.getCurrentProgress(), 0) : 0;

        if (usesBinaryClientProgress(current)) {
            return currentProgress >= target || current.getStatus() == TaskStatus.COMPLETED ? 1 : 0;
        }

        return current.getStatus() == TaskStatus.COMPLETED ? target : currentProgress;
    }

    private boolean usesBinaryClientProgress(TaskDTO current) {
        if (current == null) {
            return false;
        }
        Integer conditionType = current.getLegacyConditionType();
        if (conditionType == null) {
            conditionType = TaskConditionRegistry.resolveConditionType(current.getTaskKey());
        }
        return conditionType != null && conditionType == TaskCondition.LEVEL_UP.id();
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
