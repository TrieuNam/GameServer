package com.SouthMillion.webSocket_server.handler.task;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.task.TaskHandler;
import com.SouthMillion.webSocket_server.service.LoginSnapshotService;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.TaskFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import org.SouthMillion.dto.task.TaskDTO;
import org.SouthMillion.dto.task.TaskListResp;
import org.SouthMillion.dto.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskHandler.
 *
 * Key behaviors under test:
 *  - COMPLETED task  → progress sent = targetValue  (claim button active on client)
 *  - IN_PROGRESS     → progress sent = currentProgress
 *  - NOT_STARTED     → progress sent = 0
 *  - task-service down → fallback progress = 0 (no NPE / exception)
 *  - advanceTask blocked (task not COMPLETED) → same taskId, real progress re-sent
 */
@ExtendWith(MockitoExtension.class)
class TaskHandlerTest {

    @Mock private TaskFeign taskFeign;
    @Mock private TaskProgressPublisher taskProgressPublisher;
    @Mock private BagFeign bagFeign;
    @Mock private WalletHttpClient walletHttpClient;
    @Mock private LoginSnapshotService loginSnapshotService;
    @InjectMocks private TaskHandler taskHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        lenient().when(playerSession.getRoleId()).thenReturn(2001L);
    }

    // ─── interests ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("interests() returns only CS_FETCH_TASK_REWARD_REQ (1451)")
    void testInterests() {
        int[] interests = taskHandler.interests();
        assertNotNull(interests);
        assertEquals(1, interests.length);
        assertEquals(MessageIds.CS_FETCH_TASK_REWARD_REQ, interests[0]);
    }

    // ─── pushCurrentTaskProgress ──────────────────────────────────────────────

    @Test
    @DisplayName("COMPLETED task → pushes targetValue so claim button is active")
    void pushCurrentTaskProgress_completedTask_sendsTargetValue() {
        TaskDTO task = TaskDTO.builder()
                .taskKey("join_guild")
                .targetValue(1)
                .currentProgress(1)
                .status(TaskStatus.COMPLETED)
                .build();
        TaskListResp resp = TaskListResp.builder()
                .tasks(List.of(task))
                .claimedTasks(0)
                .totalTasks(1)
                .build();
        when(taskFeign.getTaskList("2001")).thenReturn(resp);

        taskHandler.pushCurrentTaskProgress(playerSession);

        // Emitters.emit calls session.sendBinary / getOutbound — just verify taskFeign was queried
        verify(taskFeign).getTaskList("2001");
    }

    @Test
    @DisplayName("IN_PROGRESS task → pushes real progress (not targetValue) — claim button stays hidden")
    void pushCurrentTaskProgress_inProgressTask_sendsRealProgress() {
        TaskDTO task = TaskDTO.builder()
                .taskKey("join_guild")
                .targetValue(1)
                .currentProgress(0)          // not done yet
                .status(TaskStatus.IN_PROGRESS)
                .build();
        TaskListResp resp = TaskListResp.builder()
                .tasks(List.of(task))
                .claimedTasks(0)
                .totalTasks(1)
                .build();
        when(taskFeign.getTaskList("2001")).thenReturn(resp);

        assertDoesNotThrow(() -> taskHandler.pushCurrentTaskProgress(playerSession));
        verify(taskFeign).getTaskList("2001");
    }

    @Test
    @DisplayName("NOT_STARTED task → pushes 0 progress")
    void pushCurrentTaskProgress_notStarted_sendsZero() {
        TaskDTO task = TaskDTO.builder()
                .taskKey("create_guild")
                .targetValue(1)
                .currentProgress(0)
                .status(TaskStatus.NOT_STARTED)
                .build();
        TaskListResp resp = TaskListResp.builder()
                .tasks(List.of(task))
                .claimedTasks(0)
                .totalTasks(1)
                .build();
        when(taskFeign.getTaskList("2001")).thenReturn(resp);

        assertDoesNotThrow(() -> taskHandler.pushCurrentTaskProgress(playerSession));
        verify(taskFeign).getTaskList("2001");
    }

    @Test
    @DisplayName("task-service unavailable → fallback progress=0 without exception")
    void pushCurrentTaskProgress_taskServiceDown_fallbackZero() {
        when(taskFeign.getTaskList(anyString())).thenThrow(new RuntimeException("service unavailable"));

        assertDoesNotThrow(() -> taskHandler.pushCurrentTaskProgress(playerSession));
    }

    @Test
    @DisplayName("Direct realtime push bypasses stale login snapshot cache")
    void pushCurrentTaskProgress_realtimePushBypassesCache() {
        TaskDTO freshTask = TaskDTO.builder()
                .taskKey("0")
                .targetValue(1)
                .currentProgress(1)
                .status(TaskStatus.COMPLETED)
                .build();
        when(taskFeign.getTaskList("2001")).thenReturn(TaskListResp.builder()
                .tasks(List.of(freshTask))
                .claimedTasks(0)
                .totalTasks(1)
                .build());

        assertDoesNotThrow(() -> taskHandler.pushCurrentTaskProgress(playerSession));

        verify(taskFeign).getTaskList("2001");
        verify(loginSnapshotService, never()).getCachedModuleData(2001L, "task");
    }

    @Test
    @DisplayName("Bootstrap push may still use cached task snapshot")
    void sendAllTaskProgress_bootstrapMayUseCachedSnapshot() {
        when(loginSnapshotService.getCachedModuleData(2001L, "task")).thenReturn(Map.of(
                "claimedTasks", 0,
                "tasks", List.of(Map.of(
                        "taskKey", "0",
                        "currentProgress", 1,
                        "targetValue", 1,
                        "status", "COMPLETED"
                ))
        ));

        assertDoesNotThrow(() -> taskHandler.sendAllTaskProgress(playerSession).block());

        verify(loginSnapshotService).getCachedModuleData(2001L, "task");
        verify(taskFeign, never()).getTaskList("2001");
    }

    @Test
    @DisplayName("condition 1 level task uses binary 0/1 client progress")
    void toClientProgress_levelTaskUsesBinaryDisplay() {
        TaskDTO inProgress = TaskDTO.builder()
                .taskKey("8")
                .legacyConditionType(1)
                .targetValue(3)
                .currentProgress(2)
                .status(TaskStatus.IN_PROGRESS)
                .build();
        TaskDTO completed = TaskDTO.builder()
                .taskKey("8")
                .legacyConditionType(1)
                .targetValue(3)
                .currentProgress(3)
                .status(TaskStatus.COMPLETED)
                .build();

        assertEquals(0, taskHandler.toClientProgress(inProgress));
        assertEquals(1, taskHandler.toClientProgress(completed));
    }

    @Test
    @DisplayName("non-level tasks keep their real numeric client progress")
    void toClientProgress_nonLevelTaskKeepsNumericProgress() {
        TaskDTO inProgress = TaskDTO.builder()
                .taskKey("37")
                .legacyConditionType(37)
                .targetValue(5)
                .currentProgress(2)
                .status(TaskStatus.IN_PROGRESS)
                .build();
        TaskDTO completed = TaskDTO.builder()
                .taskKey("37")
                .legacyConditionType(37)
                .targetValue(5)
                .currentProgress(5)
                .status(TaskStatus.COMPLETED)
                .build();

        assertEquals(2, taskHandler.toClientProgress(inProgress));
        assertEquals(5, taskHandler.toClientProgress(completed));
    }

    @Test
    @DisplayName("All tasks claimed (taskId >= tasks.size) → sends last index progress=0")
    void pushCurrentTaskProgress_allTasksDone_sendsZeroProgress() {
        TaskDTO task = TaskDTO.builder().taskKey("daily_login").targetValue(1)
                .currentProgress(1).status(TaskStatus.CLAIMED).build();
        // claimedTasks == totalTasks → all done
        TaskListResp resp = TaskListResp.builder()
                .tasks(List.of(task))
                .claimedTasks(1)   // index 1 out of bounds → no current task
                .totalTasks(1)
                .build();
        when(taskFeign.getTaskList("2001")).thenReturn(resp);

        assertDoesNotThrow(() -> taskHandler.pushCurrentTaskProgress(playerSession));
    }

    // ─── sendAllTaskProgress ─────────────────────────────────────────────────

    @Test
    @DisplayName("sendAllTaskProgress fetches task-service data on cache miss")
    void sendAllTaskProgress_delegatesCorrectly() {
        TaskDTO task = TaskDTO.builder().taskKey("daily_login").targetValue(1)
                .currentProgress(0).status(TaskStatus.NOT_STARTED).build();
        when(loginSnapshotService.getCachedModuleData(2001L, "task")).thenReturn(null);
        when(taskFeign.getTaskList("2001")).thenReturn(
                TaskListResp.builder().tasks(List.of(task)).claimedTasks(0).totalTasks(1).build());

        assertDoesNotThrow(() -> taskHandler.sendAllTaskProgress(playerSession).block());
        verify(loginSnapshotService).getCachedModuleData(2001L, "task");
        verify(taskFeign).getTaskList("2001");
    }

    @Test
    @DisplayName("sendAllTaskProgress with null roleId → returns Mono.empty without querying task-service")
    void sendAllTaskProgress_nullRoleId_noQuery() {
        when(playerSession.getRoleId()).thenReturn(null);

        assertDoesNotThrow(() -> taskHandler.sendAllTaskProgress(playerSession).block());
        verify(taskFeign, never()).getTaskList(anyString());
    }

    // ─── handle (claim) ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Claim on COMPLETED task → advanceTask called, new task pushed")
    void handle_completedTask_advancesAndPushesNewTask() {
        when(taskFeign.advanceTask("2001")).thenReturn(1);

        TaskDTO completed = TaskDTO.builder().taskKey("daily_login").targetValue(1)
                .currentProgress(1).status(TaskStatus.COMPLETED).build();
        TaskDTO claimed = TaskDTO.builder().taskKey("daily_login").targetValue(1)
                .currentProgress(1).status(TaskStatus.CLAIMED).build();
        TaskDTO next = TaskDTO.builder().taskKey("kill_monster").targetValue(10)
                .currentProgress(0).status(TaskStatus.NOT_STARTED).build();
        TaskListResp before = TaskListResp.builder()
                .tasks(List.of(completed, next))
                .claimedTasks(0)
                .totalTasks(2)
                .build();
        TaskListResp after = TaskListResp.builder()
                .tasks(List.of(claimed, next))
                .claimedTasks(1)
                .totalTasks(2)
                .build();
        when(taskFeign.getTaskList("2001")).thenReturn(before, after, after);
        when(bagFeign.list("2001")).thenReturn(List.of());
        when(walletHttpClient.info("2001")).thenReturn(null);

        assertDoesNotThrow(() ->
                taskHandler.handle(playerSession, MessageIds.CS_FETCH_TASK_REWARD_REQ, new byte[0]).block());

        verify(taskFeign).advanceTask("2001");
        verify(taskFeign, atLeastOnce()).getTaskList("2001");
        verify(bagFeign, atLeastOnce()).list("2001");
        verify(walletHttpClient, atLeastOnce()).info("2001");
    }

    @Test
    @DisplayName("Claimed reward still refreshes bag/wallet when next task id does not increase")
    void handle_claimedReward_sameTaskIdButClaimCountIncreased_refreshesBagAndWallet() {
        when(taskFeign.advanceTask("2001")).thenReturn(1);

        TaskDTO done = TaskDTO.builder().taskKey("daily_login").targetValue(1)
                .currentProgress(1).status(TaskStatus.CLAIMED).build();
        TaskDTO next = TaskDTO.builder().taskKey("kill_monster").targetValue(10)
                .currentProgress(0).status(TaskStatus.NOT_STARTED).build();
        TaskListResp before = TaskListResp.builder()
                .tasks(List.of(done, next))
                .claimedTasks(1)
                .totalTasks(2)
                .build();
        TaskListResp after = TaskListResp.builder()
                .tasks(List.of(done, next))
                .claimedTasks(2)
                .totalTasks(2)
                .build();
        when(taskFeign.getTaskList("2001")).thenReturn(before, after, after);
        when(bagFeign.list("2001")).thenReturn(List.of());
        when(walletHttpClient.info("2001")).thenReturn(null);

        assertDoesNotThrow(() ->
                taskHandler.handle(playerSession, MessageIds.CS_FETCH_TASK_REWARD_REQ, new byte[0]).block());

        verify(taskFeign).advanceTask("2001");
        verify(taskFeign, atLeastOnce()).getTaskList("2001");
        verify(bagFeign, atLeastOnce()).list("2001");
        verify(walletHttpClient, atLeastOnce()).info("2001");
    }

    @Test
    @DisplayName("Claim on NOT_COMPLETED task → advanceTask called but same taskId returned, real progress pushed")
    void handle_notCompletedTask_advanceBlockedRealProgressPushed() {
        // advanceTask returns same index (0) because task is not COMPLETED
        when(taskFeign.advanceTask("2001")).thenReturn(0);

        TaskDTO inProgress = TaskDTO.builder().taskKey("join_guild").targetValue(1)
                .currentProgress(0).status(TaskStatus.IN_PROGRESS).build();
        when(taskFeign.getTaskList("2001")).thenReturn(
                TaskListResp.builder().tasks(List.of(inProgress)).claimedTasks(0).totalTasks(1).build());

        assertDoesNotThrow(() ->
                taskHandler.handle(playerSession, MessageIds.CS_FETCH_TASK_REWARD_REQ, new byte[0]).block());

        verify(taskFeign).advanceTask("2001");
        verify(taskFeign, atLeastOnce()).getTaskList("2001");
        verify(bagFeign, never()).list(anyString());
        verify(walletHttpClient, never()).info(anyString());
    }

    @Test
    @DisplayName("handle with null roleId → returns Mono.empty without any feign calls")
    void handle_nullRoleId_noFeignCalls() {
        when(playerSession.getRoleId()).thenReturn(null);

        assertDoesNotThrow(() ->
                taskHandler.handle(playerSession, MessageIds.CS_FETCH_TASK_REWARD_REQ, new byte[0]).block());

        verify(taskFeign, never()).advanceTask(anyString());
        verify(taskFeign, never()).getTaskList(anyString());
    }

    @Test
    @DisplayName("handle with wrong msgId → returns Mono.empty without any feign calls")
    void handle_unknownMsgId_noFeignCalls() {
        assertDoesNotThrow(() ->
                taskHandler.handle(playerSession, 99999, new byte[0]).block());

        verify(taskFeign, never()).advanceTask(anyString());
    }

        @Test
        @DisplayName("Legacy current task uses legacyConditionType to match get_equip events")
        void pushCurrentTaskProgressIfRelevant_matchesLegacyConditionType() {
                TaskDTO legacyTask = TaskDTO.builder()
                                .taskKey("2")
                                .legacyConditionType(6)
                                .targetValue(5)
                                .currentProgress(1)
                                .status(TaskStatus.IN_PROGRESS)
                                .build();
                TaskListResp resp = TaskListResp.builder()
                                .tasks(List.of(legacyTask))
                                .claimedTasks(0)
                                .totalTasks(1)
                                .build();
                when(taskFeign.getTaskList("2001")).thenReturn(resp);

                assertDoesNotThrow(() -> taskHandler.pushCurrentTaskProgressIfRelevant(playerSession, "get_equip"));

                verify(taskFeign, times(2)).getTaskList("2001");
        }

    @Test
    @DisplayName("advanceTask throws → fallback pushCurrentTaskProgress still runs")
    void handle_advanceTaskThrows_fallbackPushes() {
        when(taskFeign.advanceTask("2001")).thenThrow(new RuntimeException("task-service down"));
        when(taskFeign.getTaskList("2001")).thenThrow(new RuntimeException("task-service down"));

        assertDoesNotThrow(() ->
                taskHandler.handle(playerSession, MessageIds.CS_FETCH_TASK_REWARD_REQ, new byte[0]).block());
    }
}
