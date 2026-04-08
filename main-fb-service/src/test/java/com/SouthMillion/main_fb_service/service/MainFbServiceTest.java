package com.SouthMillion.main_fb_service.service;

import com.SouthMillion.main_fb_service.entity.MainFbChapterRewardEntity;
import com.SouthMillion.main_fb_service.entity.MainFbStageProgressEntity;
import com.SouthMillion.main_fb_service.entity.MainFbTaskProgressEntity;
import com.SouthMillion.main_fb_service.repository.MainFbChapterRewardRepository;
import com.SouthMillion.main_fb_service.repository.MainFbStageProgressRepository;
import com.SouthMillion.main_fb_service.repository.MainFbTaskProgressRepository;
import com.SouthMillion.main_fb_service.service.client.GameworldFeignClient;
import com.SouthMillion.main_fb_service.service.client.ItemFeignClient;
import com.SouthMillion.main_fb_service.service.remote.MainFbTaskConfigService;
import com.SouthMillion.main_fb_service.service.remote.MaoxianConfig;
import com.SouthMillion.main_fb_service.service.remote.RemoteMaoxianConfigService;
import org.SouthMillion.dto.main_fb.ItemStackDTO;
import org.SouthMillion.dto.main_fb.MainFbDTOs;
import org.SouthMillion.dto.main_fb.TaskResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainFbService Tests")
class MainFbServiceTest {

    @Mock private RemoteMaoxianConfigService cfg;
    @Mock private MainFbTaskConfigService    taskCfg;
    @Mock private ItemFeignClient            itemFeign;
    @Mock private GameworldFeignClient       gameworldFeign;
    @Mock private MainFbStageProgressRepository progressRepo;
    @Mock private MainFbChapterRewardRepository chapterRepo;
    @Mock private MainFbTaskProgressRepository  taskRepo;
    @Mock private TaskProgressPublisher         taskProgressPublisher;
    @Mock private StringRedisTemplate           redis;
    @SuppressWarnings("unchecked")
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private MainFbService mainFbService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mainFbService, "STAMINA_ITEM_ID", 50001L);
        Mockito.lenient().when(taskProgressPublisher.publish(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(true);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static MaoxianConfig.Clearance clearance(int stage, int level, int cost) {
        return MaoxianConfig.Clearance.builder()
                .stage(String.valueOf(stage))
                .level(String.valueOf(level))
                .score("100")
                .now_box(String.valueOf(cost))
                .quick_num("3")
                .quick_expend("10|10|10")
                .win(List.of(new MaoxianConfig.Drop("50001", "10")))
                .build();
    }

    private static MainFbStageProgressEntity progress(String playerId, int stage, int level,
                                                       int bestScore, int clearCount) {
        return MainFbStageProgressEntity.builder()
                .playerId(playerId).stage(stage).level(level)
                .bestScore(bestScore).clearCount(clearCount)
                .build();
    }

    private static MainFbTaskConfigService.TaskCfg taskCfgEntry(int index, int stage, int level) {
        return MainFbTaskConfigService.TaskCfg.builder()
                .index(index).stage(stage).level(level)
                .threeStarScore(100)
                .rewardPreview(List.of(new MaoxianConfig.Drop("50001", "10")))
                .build();
    }

    // =========================================================
    // getCurrentTask()
    // =========================================================
    @Nested
    @DisplayName("getCurrentTask()")
    class GetCurrentTask {

        @Test
        @DisplayName("TC-MAINFB-001 [P] Chua co tien do – tra ve nhiem vu dau tien")
        void getCurrentTask_noProgress_returnsFirstTask() {
            MainFbTaskConfigService.TaskCfg first = taskCfgEntry(1, 1, 1);
            given(taskRepo.findByPlayerId("p1")).willReturn(Optional.empty());
            given(taskCfg.firstIndex()).willReturn(1);
            given(taskCfg.byIndex(1)).willReturn(Optional.of(first));

            TaskResp result = mainFbService.getCurrentTask("p1");

            assertThat(result.getAllDone()).isFalse();
            assertThat(result.getStage()).isEqualTo(1);
            assertThat(result.getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-MAINFB-002 [P] Co tien do doneIndex=2 – tra ve nhiem vu so 3")
        void getCurrentTask_withProgress_returnsNextTask() {
            MainFbTaskProgressEntity state = MainFbTaskProgressEntity.builder()
                    .playerId("p1").doneIndex(2).updatedAt(0L).build();
            MainFbTaskConfigService.TaskCfg next = taskCfgEntry(3, 1, 3);
            given(taskRepo.findByPlayerId("p1")).willReturn(Optional.of(state));
            given(taskCfg.byIndex(3)).willReturn(Optional.of(next));

            TaskResp result = mainFbService.getCurrentTask("p1");

            assertThat(result.getAllDone()).isFalse();
            assertThat(result.getIndex()).isEqualTo(3);
        }

        @Test
        @DisplayName("TC-MAINFB-003 [P] Hoan thanh tat ca – tra ve allDone=true")
        void getCurrentTask_allDone_returnsAllDoneTrue() {
            MainFbTaskProgressEntity state = MainFbTaskProgressEntity.builder()
                    .playerId("p1").doneIndex(10).updatedAt(0L).build();
            given(taskRepo.findByPlayerId("p1")).willReturn(Optional.of(state));
            given(taskCfg.byIndex(11)).willReturn(Optional.empty());

            TaskResp result = mainFbService.getCurrentTask("p1");

            assertThat(result.getAllDone()).isTrue();
        }
    }

    // =========================================================
    // getProgress()
    // =========================================================
    @Nested
    @DisplayName("getProgress()")
    class GetProgress {

        @Test
        @DisplayName("TC-MAINFB-004 [P] Tra ve danh sach tien do cua nguoi choi")
        void getProgress_returnsProgressList() {
            List<MainFbStageProgressEntity> entities = List.of(
                    progress("p1", 1, 1, 100, 3),
                    progress("p1", 1, 2, 80,  1)
            );
            given(progressRepo.findByPlayerId("p1")).willReturn(entities);

            MainFbDTOs.ProgressResp result = mainFbService.getProgress("p1");

            assertThat(result.getProgresses()).hasSize(2);
            assertThat(result.getProgresses().get(0).getStage()).isEqualTo(1);
            assertThat(result.getProgresses().get(0).getLevel()).isEqualTo(1);
            assertThat(result.getProgresses().get(0).getBestScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("TC-MAINFB-005 [P] Chua co tien do – tra ve list rong")
        void getProgress_noProgress_returnsEmpty() {
            given(progressRepo.findByPlayerId("p1")).willReturn(List.of());

            MainFbDTOs.ProgressResp result = mainFbService.getProgress("p1");

            assertThat(result.getProgresses()).isEmpty();
        }
    }

    // =========================================================
    // enter()
    // =========================================================
    @Nested
    @DisplayName("enter()")
    class Enter {

        @Test
        @DisplayName("TC-MAINFB-006 [P] Vao ai 1-1, khong mat the – tao battle instance")
        void enter_stage1Level1_noCost_createsBattleInstance() {
            MaoxianConfig.Clearance c = clearance(1, 1, 0);
            MainFbDTOs.EnterStageReq req = MainFbDTOs.EnterStageReq.builder()
                    .playerId("p1").stage(1).level(1).build();
            GameworldFeignClient.CreateInstanceResp resp =
                    new GameworldFeignClient.CreateInstanceResp("battle-abc", 42L, 9999999L);

            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(gameworldFeign.createInstance(any())).willReturn(resp);

            MainFbDTOs.EnterStageResp result = mainFbService.enter(req);

            assertThat(result.getBattleId()).isEqualTo("battle-abc");
            assertThat(result.getCost()).isEqualTo(0);
            then(itemFeign).should(never()).consumeItem(any());
        }

        @Test
        @DisplayName("TC-MAINFB-007 [P] Vao ai co the - tru the va tao battle")
        void enter_withStaminaCost_deductsStamina() {
            MaoxianConfig.Clearance c = clearance(1, 1, 10);
            MainFbDTOs.EnterStageReq req = MainFbDTOs.EnterStageReq.builder()
                    .playerId("p1").stage(1).level(1).build();
            GameworldFeignClient.CreateInstanceResp resp =
                    new GameworldFeignClient.CreateInstanceResp("battle-xyz", 7L, 9999999L);

            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(gameworldFeign.createInstance(any())).willReturn(resp);

            MainFbDTOs.EnterStageResp result = mainFbService.enter(req);

            assertThat(result.getCost()).isEqualTo(10);
            then(itemFeign).should().consumeItem(any());
        }

        @Test
        @DisplayName("TC-MAINFB-008 [N] Stage/Level khong ton tai – nem IllegalArgumentException")
        void enter_stageNotFound_throws() {
            MainFbDTOs.EnterStageReq req = MainFbDTOs.EnterStageReq.builder()
                    .playerId("p1").stage(99).level(99).build();
            given(cfg.findStage(99, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mainFbService.enter(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stage/Level");
        }

        @Test
        @DisplayName("TC-MAINFB-009 [N] Ai chua duoc mo khoa – nem IllegalStateException")
        void enter_levelNotUnlocked_throws() {
            MaoxianConfig.Clearance c = clearance(1, 2, 0);
            MainFbDTOs.EnterStageReq req = MainFbDTOs.EnterStageReq.builder()
                    .playerId("p1").stage(1).level(2).build();

            given(cfg.findStage(1, 2)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 1))
                    .willReturn(Optional.empty()); // level 1 not cleared

            assertThatThrownBy(() -> mainFbService.enter(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Chưa mở khoá");
        }
    }

    // =========================================================
    // finish()
    // =========================================================
    @Nested
    @DisplayName("finish()")
    class Finish {

        @Test
        @DisplayName("TC-MAINFB-010 [P] Ket qua da duoc xu ly truoc do – tra ve list rong (idempotency)")
        void finish_alreadySettled_returnsEmpty() {
            MainFbDTOs.FinishStageReq req = MainFbDTOs.FinishStageReq.builder()
                    .playerId("p1").battleId("battle-1").stage(1).level(1).score(120).build();
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(30))))
                    .willReturn(Boolean.FALSE);

            List<ItemStackDTO> result = mainFbService.finish(req);

            assertThat(result).isEmpty();
            then(cfg).should(never()).findStage(anyInt(), anyInt());
        }

        @Test
        @DisplayName("TC-MAINFB-011 [P] Lan clear dau – phan thuong nhan doi")
        void finish_firstClear_doublesRewards() {
            MaoxianConfig.Clearance c = clearance(1, 1, 0);
            MainFbDTOs.FinishStageReq req = MainFbDTOs.FinishStageReq.builder()
                    .playerId("p1").battleId("battle-1").stage(1).level(1).score(150).build();

            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(30))))
                    .willReturn(Boolean.TRUE);
            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 1))
                    .willReturn(Optional.empty()); // first clear
            given(progressRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(taskCfg.indexOf(1, 1)).willReturn(-1); // not in task list

            List<ItemStackDTO> result = mainFbService.finish(req);

            // Win rewards = 1 drop of 10, doubled on first clear = 20 total
            assertThat(result).isNotEmpty();
            long totalCount = result.stream().mapToLong(ItemStackDTO::count).sum();
            assertThat(totalCount).isEqualTo(20); // 10 doubled
            then(itemFeign).should().addItemsBulk(any());
        }

        @Test
        @DisplayName("TC-MAINFB-011A [P] condition 4 nhan snapshot so ai da clear thay vi delta=1")
        void finish_reportsAbsoluteDungeonProgressSnapshot() {
            MaoxianConfig.Clearance c = clearance(1, 2, 0);
            MainFbDTOs.FinishStageReq req = MainFbDTOs.FinishStageReq.builder()
                    .playerId("p1").battleId("battle-2").stage(1).level(2).score(150).build();

            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(30))))
                    .willReturn(Boolean.TRUE);
            given(cfg.findStage(1, 2)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 2))
                    .willReturn(Optional.empty());
            given(progressRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(progressRepo.findByPlayerId("p1")).willReturn(List.of(
                    progress("p1", 1, 1, 150, 1),
                    progress("p1", 1, 2, 150, 1)
            ));
            given(taskCfg.indexOf(1, 2)).willReturn(-1); // không nằm trong chain nhiệm vụ nội bộ vẫn phải báo task condition_4

            mainFbService.finish(req);

            then(taskProgressPublisher).should().publish("p1", "complete_dungeon", 2, "main-fb-service");
        }

        @Test
        @DisplayName("TC-MAINFB-012 [P] Clear lan 2+ – phan thuong khong nhan doi")
        void finish_subsequentClear_normalRewards() {
            MaoxianConfig.Clearance c = clearance(1, 1, 0);
            MainFbDTOs.FinishStageReq req = MainFbDTOs.FinishStageReq.builder()
                    .playerId("p1").battleId("battle-1").stage(1).level(1).score(150).build();
            MainFbStageProgressEntity existing = progress("p1", 1, 1, 120, 2);

            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(30))))
                    .willReturn(Boolean.TRUE);
            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 1))
                    .willReturn(Optional.of(existing));
            given(progressRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(taskCfg.indexOf(1, 1)).willReturn(-1);

            List<ItemStackDTO> result = mainFbService.finish(req);

            assertThat(result).isNotEmpty();
            long totalCount = result.stream().mapToLong(ItemStackDTO::count).sum();
            assertThat(totalCount).isEqualTo(10); // no doubling
        }

        @Test
        @DisplayName("TC-MAINFB-013 [N] Stage/Level khong ton tai – nem IllegalArgumentException")
        void finish_stageNotFound_throws() {
            MainFbDTOs.FinishStageReq req = MainFbDTOs.FinishStageReq.builder()
                    .playerId("p1").battleId("battle-1").stage(99).level(99).score(100).build();

            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(30))))
                    .willReturn(Boolean.TRUE);
            given(cfg.findStage(99, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mainFbService.finish(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stage/Level");
        }
    }

    // =========================================================
    // sweep()
    // =========================================================
    @Nested
    @DisplayName("sweep()")
    class Sweep {

        @Test
        @DisplayName("TC-MAINFB-014 [P] Quet hop le – tra ve phan thuong")
        void sweep_validSweep_returnsRewards() {
            MaoxianConfig.Clearance c = clearance(1, 1, 0);
            MainFbDTOs.SweepReq req = MainFbDTOs.SweepReq.builder()
                    .playerId("p1").stage(1).level(1).times(2).build();
            MainFbStageProgressEntity prog = progress("p1", 1, 1, 150, 3); // bestScore >= 100

            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 1))
                    .willReturn(Optional.of(prog));

            MainFbDTOs.SweepResp result = mainFbService.sweep(req);

            assertThat(result.getRewards()).isNotEmpty();
            // 2 times × 10 items = 20
            int total = result.getRewards().stream().mapToInt(MainFbDTOs.RewardItem::getCount).sum();
            assertThat(total).isEqualTo(20);
            then(itemFeign).should().addItemsBulk(any());
        }

        @Test
        @DisplayName("TC-MAINFB-015 [N] Diem khong du 3 sao – nem IllegalStateException")
        void sweep_notEnoughScore_throws() {
            MaoxianConfig.Clearance c = clearance(1, 1, 0);
            MainFbDTOs.SweepReq req = MainFbDTOs.SweepReq.builder()
                    .playerId("p1").stage(1).level(1).times(1).build();
            MainFbStageProgressEntity prog = progress("p1", 1, 1, 50, 1); // bestScore < 100

            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 1))
                    .willReturn(Optional.of(prog));

            assertThatThrownBy(() -> mainFbService.sweep(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("3 sao");
        }

        @Test
        @DisplayName("TC-MAINFB-016 [N] Chua tung clear ai – nem IllegalArgumentException")
        void sweep_neverCleared_throws() {
            MaoxianConfig.Clearance c = clearance(1, 1, 0);
            MainFbDTOs.SweepReq req = MainFbDTOs.SweepReq.builder()
                    .playerId("p1").stage(1).level(1).times(1).build();

            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 1))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> mainFbService.sweep(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Chưa từng vượt");
        }

        @Test
        @DisplayName("TC-MAINFB-017 [P] So lan quet vuot gioi han – gioi han theo quick_num")
        void sweep_timesExceedsMax_capsAtMaxQuickNum() {
            MaoxianConfig.Clearance c = clearance(1, 1, 0);
            // quick_num = 3, so max 3 sweeps even if times=5
            MainFbDTOs.SweepReq req = MainFbDTOs.SweepReq.builder()
                    .playerId("p1").stage(1).level(1).times(5).build();
            MainFbStageProgressEntity prog = progress("p1", 1, 1, 150, 3);

            given(cfg.findStage(1, 1)).willReturn(Optional.of(c));
            given(progressRepo.findByPlayerIdAndStageAndLevel("p1", 1, 1))
                    .willReturn(Optional.of(prog));

            MainFbDTOs.SweepResp result = mainFbService.sweep(req);

            // max 3 times × 10 items = 30 (capped)
            int total = result.getRewards().stream().mapToInt(MainFbDTOs.RewardItem::getCount).sum();
            assertThat(total).isEqualTo(30);
        }
    }

    // =========================================================
    // claimChapterReward()
    // =========================================================
    @Nested
    @DisplayName("claimChapterReward()")
    class ClaimChapterReward {

        private MaoxianConfig buildConfig() {
            MaoxianConfig.JieduanReward jr = MaoxianConfig.JieduanReward.builder()
                    .stage("1")
                    .chapter("chapter1")
                    .clearance_condition("3")
                    .win(List.of(new MaoxianConfig.Drop("50001", "100")))
                    .build();
            return MaoxianConfig.builder()
                    .clearance(List.of())
                    .jieduan_reward(List.of(jr))
                    .build();
        }

        @Test
        @DisplayName("TC-MAINFB-018 [P] Du dieu kien nhan thuong – phan thuong duoc cap phat")
        void claimChapterReward_eligible_grantsRewards() {
            given(cfg.getConfig()).willReturn(buildConfig());
            given(chapterRepo.findByPlayerIdAndStageAndChapterLabel("p1", 1, "chapter1"))
                    .willReturn(Optional.empty());
            // 3 cleared stages
            List<MainFbStageProgressEntity> cleared = List.of(
                    progress("p1", 1, 1, 100, 1),
                    progress("p1", 1, 2, 100, 1),
                    progress("p1", 1, 3, 100, 1)
            );
            given(progressRepo.findByPlayerId("p1")).willReturn(cleared);
            given(chapterRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            List<ItemStackDTO> result = mainFbService.claimChapterReward("p1", 1, "chapter1");

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).count()).isEqualTo(100);
            then(itemFeign).should().addItemsBulk(any());
        }

        @Test
        @DisplayName("TC-MAINFB-019 [N] Da nhan truoc do – nem IllegalStateException")
        void claimChapterReward_alreadyClaimed_throws() {
            given(cfg.getConfig()).willReturn(buildConfig());
            MainFbChapterRewardEntity claimed = MainFbChapterRewardEntity.builder()
                    .playerId("p1").stage(1).chapterLabel("chapter1").claimed(true).build();
            given(chapterRepo.findByPlayerIdAndStageAndChapterLabel("p1", 1, "chapter1"))
                    .willReturn(Optional.of(claimed));

            assertThatThrownBy(() -> mainFbService.claimChapterReward("p1", 1, "chapter1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Đã nhận");
        }

        @Test
        @DisplayName("TC-MAINFB-020 [N] Chua du so ai da clear – nem IllegalStateException")
        void claimChapterReward_notEnoughStagesCleared_throws() {
            given(cfg.getConfig()).willReturn(buildConfig());
            given(chapterRepo.findByPlayerIdAndStageAndChapterLabel("p1", 1, "chapter1"))
                    .willReturn(Optional.empty());
            // Only 2 cleared stages (need 3)
            List<MainFbStageProgressEntity> cleared = List.of(
                    progress("p1", 1, 1, 100, 1),
                    progress("p1", 1, 2, 100, 1)
            );
            given(progressRepo.findByPlayerId("p1")).willReturn(cleared);

            assertThatThrownBy(() -> mainFbService.claimChapterReward("p1", 1, "chapter1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Chưa đủ");
        }

        @Test
        @DisplayName("TC-MAINFB-021 [N] Chapter khong ton tai trong config – nem IllegalArgumentException")
        void claimChapterReward_invalidChapter_throws() {
            given(cfg.getConfig()).willReturn(buildConfig());

            assertThatThrownBy(() -> mainFbService.claimChapterReward("p1", 1, "nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không có mốc");
        }
    }
}
