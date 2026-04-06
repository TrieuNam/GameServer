package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.client.BagClient;
import com.SouthMillion.task_service.client.WalletClient;
import com.SouthMillion.task_service.entity.TaskProgressEntity;
import com.SouthMillion.task_service.repository.TaskProgressEventRepository;
import com.SouthMillion.task_service.repository.TaskProgressRepository;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.event.task.TaskProgressEvent;
import org.SouthMillion.dto.task.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskDomainService Tests")
class TaskDomainServiceTest {

    @Mock
    private TaskProgressRepository taskProgressRepository;

    @Mock
    private TaskProgressEventRepository taskProgressEventRepository;

    @Mock
    private WalletClient walletClient;

    @Mock
    private BagClient bagClient;

    @InjectMocks
    private TaskDomainService taskDomainService;

    private static final String PLAYER_ID = "1";
    private static final Long   PLAYER_ID_L = 1L;

    // ── helpers ──────────────────────────────────────────────
    private TaskProgressEntity progress(String taskKey, int value, TaskStatus status) {
        return TaskProgressEntity.builder()
                .playerId(PLAYER_ID_L)
                .taskKey(taskKey)
                .progressValue(value)
                .status(status)
                .build();
    }

    // =========================================================
    // getAllTasks
    // =========================================================
    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test
        @DisplayName("TC-TSK-001 [P] Lay danh sach nhiem vu – tra ve tat ca tasks")
        void getAllTasks_returnsAllConfiguredTasks() {
            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L)).willReturn(List.of());

            TaskListResp resp = taskDomainService.getAllTasks(PLAYER_ID);

            assertThat(resp.getTotalTasks()).isEqualTo(9);
            assertThat(resp.getTasks()).hasSize(9);
        }

        @Test
        @DisplayName("TC-TSK-002 [P] Player moi – tat ca task la NOT_STARTED")
        void getAllTasks_newPlayer_allNotStarted() {
            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L)).willReturn(List.of());

            TaskListResp resp = taskDomainService.getAllTasks(PLAYER_ID);

            assertThat(resp.getTasks())
                    .allMatch(t -> t.getStatus() == TaskStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("TC-TSK-003 [P] Player co task dang lam – IN_PROGRESS hien thi dung progress")
        void getAllTasks_withProgress_showsCorrectStatus() {
            TaskProgressEntity prog = progress("kill_monster", 5, TaskStatus.IN_PROGRESS);
            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L)).willReturn(List.of(prog));

            TaskListResp resp = taskDomainService.getAllTasks(PLAYER_ID);

            TaskDTO killTask = resp.getTasks().stream()
                    .filter(t -> "kill_monster".equals(t.getTaskKey()))
                    .findFirst()
                    .orElseThrow();

            assertThat(killTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(killTask.getCurrentProgress()).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-TSK-003b [P] completedTasks va claimedTasks duoc dem dung")
        void getAllTasks_countsCorrectly() {
            List<TaskProgressEntity> list = List.of(
                    progress("kill_monster", 10, TaskStatus.COMPLETED),
                    progress("daily_login", 1, TaskStatus.CLAIMED)
            );
            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L)).willReturn(list);

            TaskListResp resp = taskDomainService.getAllTasks(PLAYER_ID);

            // COMPLETED + CLAIMED deu tinh vao completedCount; chi CLAIMED tinh vao claimedCount
            assertThat(resp.getCompletedTasks()).isEqualTo(2);
            assertThat(resp.getClaimedTasks()).isEqualTo(1);
        }

        @Test
        @DisplayName("getAllTasks returns numeric task ids in ascending order")
        void getAllTasks_numericKeys_sortedNumerically() {
            TaskDefinitionProvider provider = mock(TaskDefinitionProvider.class);
            Map<String, TaskDefinitionConfig> configs = Map.of(
                "10", new TaskDefinitionConfig("10", "t10", "", 1, 0, 0, ""),
                "2", new TaskDefinitionConfig("2", "t2", "", 1, 0, 0, ""),
                "1", new TaskDefinitionConfig("1", "t1", "", 1, 0, 0, "")
            );
            given(provider.getTaskConfigs()).willReturn(configs);
            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L)).willReturn(List.of());

            TaskDomainService service = new TaskDomainService(
                taskProgressRepository, taskProgressEventRepository, walletClient, bagClient, provider, mock(MeterRegistry.class));

            TaskListResp resp = service.getAllTasks(PLAYER_ID);
            List<String> keys = resp.getTasks().stream()
                .map(TaskDTO::getTaskKey)
                .collect(Collectors.toList());

            assertThat(keys).containsExactly("1", "2", "10");
        }

        @Test
        @DisplayName("getAllTasks keeps numeric-first deterministic ordering for mixed keys")
        void getAllTasks_mixedKeys_numericFirst() {
            TaskDefinitionProvider provider = mock(TaskDefinitionProvider.class);
            Map<String, TaskDefinitionConfig> configs = Map.of(
                "daily_login", new TaskDefinitionConfig("daily_login", "daily", "", 1, 0, 0, ""),
                "10", new TaskDefinitionConfig("10", "t10", "", 1, 0, 0, ""),
                "1", new TaskDefinitionConfig("1", "t1", "", 1, 0, 0, "")
            );
            given(provider.getTaskConfigs()).willReturn(configs);
            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L)).willReturn(List.of());

            TaskDomainService service = new TaskDomainService(
                taskProgressRepository, taskProgressEventRepository, walletClient, bagClient, provider, mock(MeterRegistry.class));

            TaskListResp resp = service.getAllTasks(PLAYER_ID);
            List<String> keys = resp.getTasks().stream()
                .map(TaskDTO::getTaskKey)
                .collect(Collectors.toList());

            assertThat(keys).containsExactly("1", "10", "daily_login");
        }
    }

    // =========================================================
    // reportProgress
    // =========================================================
    @Nested
    @DisplayName("reportProgress()")
    class ReportProgress {

        @Test
        @DisplayName("TC-TSK-010 [P] Bao cao tien do tang – progressValue tang dung")
        void reportProgress_incrementsValue() {
            TaskProgressEntity existing = progress("kill_monster", 3, TaskStatus.IN_PROGRESS);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(existing));
            given(taskProgressRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TaskReportReq req = TaskReportReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .progressDelta(2)
                    .build();

            taskDomainService.reportProgress(req);

            assertThat(existing.getProgressValue()).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-TSK-011 [P] Tien do dat du – status chuyen COMPLETED")
        void reportProgress_reachesTarget_becomesCompleted() {
            // kill_monster: targetValue = 10
            TaskProgressEntity existing = progress("kill_monster", 9, TaskStatus.IN_PROGRESS);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(existing));
            given(taskProgressRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TaskReportReq req = TaskReportReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .progressDelta(1)
                    .build();

            taskDomainService.reportProgress(req);

            assertThat(existing.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            assertThat(existing.getProgressValue()).isEqualTo(10);
        }

        @Test
        @DisplayName("TC-TSK-012 [P] Task chua co progress – tao moi voi status IN_PROGRESS")
        void reportProgress_noExistingProgress_createsNew() {
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "daily_login"))
                    .willReturn(Optional.empty());
            given(taskProgressRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TaskReportReq req = TaskReportReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("daily_login")
                    .progressDelta(1)
                    .build();

            taskDomainService.reportProgress(req);

            then(taskProgressRepository).should().save(argThat(
                    p -> p.getStatus() == TaskStatus.COMPLETED // daily_login target=1, delta=1
            ));
        }

        @Test
        @DisplayName("TC-TSK-013 [N] Task da CLAIMED – bao cao bi bo qua")
        void reportProgress_alreadyClaimed_ignored() {
            TaskProgressEntity claimed = progress("kill_monster", 10, TaskStatus.CLAIMED);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(claimed));

            TaskReportReq req = TaskReportReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .progressDelta(5)
                    .build();

            taskDomainService.reportProgress(req);

            // Progress khong thay doi, save khong duoc goi
            then(taskProgressRepository).should(never()).save(any());
            assertThat(claimed.getProgressValue()).isEqualTo(10);
        }

        @Test
        @DisplayName("TC-TSK-014 [N] TaskKey khong hop le – bo qua, khong loi")
        void reportProgress_unknownTaskKey_ignored() {
            TaskReportReq req = TaskReportReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("fake_task_key")
                    .progressDelta(1)
                    .build();

            assertThatCode(() -> taskDomainService.reportProgress(req))
                    .doesNotThrowAnyException();

            then(taskProgressRepository).should(never()).save(any());
        }
    }

    // =========================================================
    // claim
    // =========================================================
    @Nested
    @DisplayName("claim()")
    class Claim {

        @Test
        @DisplayName("TC-TSK-020 [P] Nhan thuong task COMPLETED – status chuyen CLAIMED")
        void claim_completed_becomesClaimedAndRewardsGranted() {
            TaskProgressEntity completed = progress("kill_monster", 10, TaskStatus.COMPLETED);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(completed));
            given(taskProgressRepository.markClaimedIfCompleted(eq(PLAYER_ID_L), eq("kill_monster"), any()))
                .willReturn(1);

            TaskClaimReq req = TaskClaimReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .build();

            taskDomainService.claim(req);

            // kill_monster co goldReward=200 → walletClient duoc goi
            then(walletClient).should().addCurrency(eq(PLAYER_ID), any());
                then(taskProgressRepository).should()
                    .markClaimedIfCompleted(eq(PLAYER_ID_L), eq("kill_monster"), any());
        }

        @Test
        @DisplayName("TC-TSK-021 [N] Task chua hoan thanh – nem IllegalStateException")
        void claim_notCompleted_throws() {
            TaskProgressEntity inProgress = progress("kill_monster", 5, TaskStatus.IN_PROGRESS);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(inProgress));

            TaskClaimReq req = TaskClaimReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .build();

            assertThatThrownBy(() -> taskDomainService.claim(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not completed");
        }

        @Test
        @DisplayName("TC-TSK-022 [N] Task da CLAIMED – nem IllegalStateException")
        void claim_alreadyClaimed_throws() {
            TaskProgressEntity claimed = progress("kill_monster", 10, TaskStatus.CLAIMED);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(claimed));

            TaskClaimReq req = TaskClaimReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .build();

            assertThatThrownBy(() -> taskDomainService.claim(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not completed");
        }

        @Test
        @DisplayName("TC-TSK-023 [N] Task khong ton tai (NOT_STARTED) – nem IllegalStateException")
        void claim_notFound_throws() {
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.empty());

            TaskClaimReq req = TaskClaimReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .build();

            assertThatThrownBy(() -> taskDomainService.claim(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Task not found");
        }

        @Test
        @DisplayName("TC-TSK-025 [P] Task co item reward – bagClient.grantItems duoc goi")
        void claim_withItemReward_bagClientCalled() {
            // kill_monster: itemRewards="item:potion:5"
            TaskProgressEntity completed = progress("kill_monster", 10, TaskStatus.COMPLETED);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(completed));
            given(taskProgressRepository.markClaimedIfCompleted(eq(PLAYER_ID_L), eq("kill_monster"), any()))
                .willReturn(1);

            TaskClaimReq req = TaskClaimReq.builder()
                    .playerId(PLAYER_ID)
                    .taskKey("kill_monster")
                    .build();

            taskDomainService.claim(req);

            then(bagClient).should().grantItems(argThat(grantReq ->
                    grantReq != null
                        && PLAYER_ID_L.equals(grantReq.getUserId())
                        && PLAYER_ID_L.equals(grantReq.getRoleId())
                            && grantReq.getIdemKey() != null
                            && grantReq.getIdemKey().startsWith("task:" + PLAYER_ID + ":kill_monster")
                            && grantReq.getItems() != null
                            && grantReq.getItems().size() == 1
                            && Integer.valueOf(101).equals(grantReq.getItems().get(0).getItemId())
                            && Integer.valueOf(5).equals(grantReq.getItems().get(0).getAmount())
            ));
        }
    }

    // =========================================================
    // claimAllCompletedTasks
    // =========================================================
    @Nested
    @DisplayName("claimAllCompletedTasks()")
    class ClaimAll {

        @Test
        @DisplayName("TC-TSK-030 [P] Claim tat ca COMPLETED tasks")
        void claimAll_multipleTasks() {
            List<TaskProgressEntity> completed = List.of(
                    progress("daily_login", 1, TaskStatus.COMPLETED),
                    progress("kill_monster", 10, TaskStatus.COMPLETED)
            );
            given(taskProgressRepository.findByPlayerIdAndStatus(PLAYER_ID_L, TaskStatus.COMPLETED))
                    .willReturn(completed);
            given(taskProgressRepository.markClaimedIfCompleted(eq(PLAYER_ID_L), anyString(), any()))
                .willReturn(1);

            taskDomainService.claimAllCompletedTasks(PLAYER_ID);

            then(taskProgressRepository).should(times(2))
                .markClaimedIfCompleted(eq(PLAYER_ID_L), anyString(), any());
        }

        @Test
        @DisplayName("TC-TSK-031 [P] Khong co task nao COMPLETED – khong lam gi")
        void claimAll_noCompletedTasks_noop() {
            given(taskProgressRepository.findByPlayerIdAndStatus(PLAYER_ID_L, TaskStatus.COMPLETED))
                    .willReturn(List.of());

            assertThatCode(() -> taskDomainService.claimAllCompletedTasks(PLAYER_ID))
                    .doesNotThrowAnyException();

            then(taskProgressRepository).should(never()).save(any());
            then(walletClient).should(never()).addCurrency(any(), any());
        }
    }

    // =========================================================
    // advanceTask
    // =========================================================
    @Nested
    @DisplayName("advanceTask()")
    class AdvanceTask {

        @Test
        @DisplayName("Numeric task_id keys are advanced in numeric order (1,2,10)")
        void advanceTask_numericKeys_sortedNumerically() {
            TaskDefinitionProvider provider = mock(TaskDefinitionProvider.class);
            Map<String, TaskDefinitionConfig> configs = Map.of(
                "10", new TaskDefinitionConfig("10", "t10", "", 1, 0, 0, ""),
                "2", new TaskDefinitionConfig("2", "t2", "", 1, 0, 0, ""),
                "1", new TaskDefinitionConfig("1", "t1", "", 1, 0, 0, "")
            );
            given(provider.getTaskConfigs()).willReturn(configs);

            TaskDomainService service = new TaskDomainService(
                taskProgressRepository, taskProgressEventRepository, walletClient, bagClient, provider, mock(MeterRegistry.class));

            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L))
                .willReturn(List.of(), List.of(), List.of(progress("1", 1, TaskStatus.CLAIMED)));
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "1"))
                .willReturn(Optional.of(progress("1", 1, TaskStatus.COMPLETED)));
            given(taskProgressRepository.markClaimedIfCompleted(eq(PLAYER_ID_L), eq("1"), any()))
                .willReturn(1);

            Integer newIndex = service.advanceTask(PLAYER_ID);

            assertThat(newIndex).isEqualTo(1);
            then(taskProgressRepository).should().findByPlayerIdAndTaskKey(PLAYER_ID_L, "1");
        }

        @Test
        @DisplayName("Mixed keys keep numeric-first deterministic ordering")
        void advanceTask_mixedKeys_numericFirst() {
            TaskDefinitionProvider provider = mock(TaskDefinitionProvider.class);
            Map<String, TaskDefinitionConfig> configs = Map.of(
                "daily_login", new TaskDefinitionConfig("daily_login", "daily", "", 1, 0, 0, ""),
                "10", new TaskDefinitionConfig("10", "t10", "", 1, 0, 0, ""),
                "1", new TaskDefinitionConfig("1", "t1", "", 1, 0, 0, "")
            );
            given(provider.getTaskConfigs()).willReturn(configs);

            TaskDomainService service = new TaskDomainService(
                taskProgressRepository, taskProgressEventRepository, walletClient, bagClient, provider, mock(MeterRegistry.class));

            given(taskProgressRepository.findAllByPlayerId(PLAYER_ID_L))
                .willReturn(
                        List.of(progress("1", 1, TaskStatus.CLAIMED)),
                        List.of(progress("1", 1, TaskStatus.CLAIMED)),
                        List.of(
                                progress("1", 1, TaskStatus.CLAIMED),
                                progress("10", 1, TaskStatus.CLAIMED)
                        )
                );
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "10"))
                .willReturn(Optional.of(progress("10", 1, TaskStatus.COMPLETED)));
            given(taskProgressRepository.markClaimedIfCompleted(eq(PLAYER_ID_L), eq("10"), any()))
                .willReturn(1);

            Integer newIndex = service.advanceTask(PLAYER_ID);

            assertThat(newIndex).isEqualTo(2);
            then(taskProgressRepository).should().findByPlayerIdAndTaskKey(PLAYER_ID_L, "10");
        }
    }

    // =========================================================
    // getTaskProgress
    // =========================================================
    @Nested
    @DisplayName("getTaskProgress()")
    class GetProgress {

        @Test
        @DisplayName("TC-TSK-P01 [P] Lay tien do dung")
        void getTaskProgress_found() {
            TaskProgressEntity prog = progress("kill_monster", 7, TaskStatus.IN_PROGRESS);
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.of(prog));

            Integer result = taskDomainService.getTaskProgress(PLAYER_ID, "kill_monster");

            assertThat(result).isEqualTo(7);
        }

        @Test
        @DisplayName("TC-TSK-P02 [P] Chua co progress – tra ve 0")
        void getTaskProgress_notFound_returnsZero() {
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "kill_monster"))
                    .willReturn(Optional.empty());

            Integer result = taskDomainService.getTaskProgress(PLAYER_ID, "kill_monster");

            assertThat(result).isEqualTo(0);
        }
    }

        // =========================================================
        // reportProgressEvent (idempotency)
        // =========================================================
        @Nested
        @DisplayName("reportProgressEvent()")
        class ReportProgressEvent {

        @Test
        @DisplayName("Unique eventId is applied exactly once")
        void reportProgressEvent_unique_appliesProgress() {
            given(taskProgressEventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(taskProgressRepository.findByPlayerIdAndTaskKey(PLAYER_ID_L, "daily_login"))
                .willReturn(Optional.empty());
            given(taskProgressRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TaskProgressEvent event = TaskProgressEvent.builder()
                .eventId("evt-unique-1")
                .roleId(PLAYER_ID_L)
                .taskKey("daily_login")
                .progressDelta(1)
                .source("test")
                .occurredAt(Instant.now())
                .build();

            boolean applied = taskDomainService.reportProgressEvent(event);

            assertThat(applied).isTrue();
            then(taskProgressEventRepository).should().save(any());
            then(taskProgressRepository).should().save(argThat(p ->
                PLAYER_ID_L.equals(p.getPlayerId())
                    && "daily_login".equals(p.getTaskKey())
                    && p.getProgressValue() == 1
                    && p.getStatus() == TaskStatus.COMPLETED));
        }

        @Test
        @DisplayName("Duplicate eventId is ignored and progress is not applied")
        void reportProgressEvent_duplicate_ignored() {
            willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(taskProgressEventRepository).save(any());

            TaskProgressEvent event = TaskProgressEvent.builder()
                .eventId("evt-dup-1")
                .roleId(PLAYER_ID_L)
                .taskKey("daily_login")
                .progressDelta(1)
                .source("test")
                .occurredAt(Instant.now())
                .build();

            boolean applied = taskDomainService.reportProgressEvent(event);

            assertThat(applied).isFalse();
            then(taskProgressRepository).should(never()).save(any());
        }
        }
}
