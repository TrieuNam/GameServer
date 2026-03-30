package com.game.escort.service.impl;

import com.SouthMillion.escort_service.client.BagClient;
import com.SouthMillion.escort_service.client.WalletClient;
import com.SouthMillion.escort_service.exception.EscortServiceException;
import com.SouthMillion.escort_service.model.entity.EscortMission;
import com.SouthMillion.escort_service.model.entity.EscortStats;
import com.SouthMillion.escort_service.repository.EscortMissionRepository;
import com.SouthMillion.escort_service.repository.EscortStatsRepository;
import com.SouthMillion.escort_service.service.impl.EscortServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EscortServiceImpl Tests")
class EscortServiceImplTest {

    @Mock private EscortMissionRepository missionRepository;
    @Mock private EscortStatsRepository   statsRepository;
    @Mock private WalletClient            walletClient;
    @Mock private BagClient               bagClient;

    @InjectMocks private EscortServiceImpl escortService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static EscortMission mission(Long userId, Long id, int quality, int status,
                                         int progress, int distance) {
        EscortMission m = new EscortMission();
        m.setUserId(userId);
        m.setId(id);
        m.setQuality(quality);
        m.setStatus(status);
        m.setProgress(progress);
        m.setDistance(distance);
        m.setRewardGold((long) (100 * quality * quality));
        m.setRewardExp((long) (50 * quality * quality));
        m.setBonusMultiplier(1.0 + quality * 0.2);
        m.setIsRewardClaimed(false);
        m.setAttacksSurvived(0);
        return m;
    }

    private static EscortStats stats(Long userId, int dailyCompleted, int dailyLimit,
                                     int freeRefreshes, LocalDateTime lastReset) {
        EscortStats s = new EscortStats();
        s.setUserId(userId);
        s.setDailyCompleted(dailyCompleted);
        s.setDailyLimit(dailyLimit);
        s.setFreeRefreshes(freeRefreshes);
        s.setLastResetDate(lastReset);
        s.setTotalCompleted(0);
        s.setTotalFailed(0);
        s.setTotalRefreshesToday(0);
        s.setBestQuality(0);
        s.setPerfectCompletions(0);
        s.setTotalDistance(0L);
        s.setTotalGoldEarned(0L);
        s.setTotalExpEarned(0L);
        return s;
    }

    // =========================================================
    // getAllMissions()
    // =========================================================
    @Nested
    @DisplayName("getAllMissions()")
    class GetAllMissions {

        @Test
        @DisplayName("TC-ESCORT-001 [P] Lay tat ca nhiem vu cua nguoi dung")
        void getAllMissions_returnsAll() {
            given(missionRepository.findByUserId(1L))
                    .willReturn(List.of(mission(1L, 10L, 1, 0, 0, 1000)));

            assertThat(escortService.getAllMissions("1")).hasSize(1);
        }
    }

    // =========================================================
    // getMission()
    // =========================================================
    @Nested
    @DisplayName("getMission()")
    class GetMission {

        @Test
        @DisplayName("TC-ESCORT-002 [P] Tim thay nhiem vu – tra ve mission")
        void getMission_found_returnsMission() {
            EscortMission m = mission(1L, 10L, 2, 0, 0, 2000);
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));

            assertThat(escortService.getMission("1", 10L)).isEqualTo(m);
        }

        @Test
        @DisplayName("TC-ESCORT-003 [N] Khong tim thay nhiem vu – nem MISSION_NOT_FOUND")
        void getMission_notFound_throws() {
            given(missionRepository.findByUserIdAndId(1L, 99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> escortService.getMission("1", 99L))
                    .isInstanceOf(EscortServiceException.class);
        }
    }

    // =========================================================
    // generateMission()
    // =========================================================
    @Nested
    @DisplayName("generateMission()")
    class GenerateMission {

        @Test
        @DisplayName("TC-ESCORT-004 [N] Quality < 1 – nem INVALID_QUALITY")
        void generateMission_qualityTooLow_throws() {
            assertThatThrownBy(() -> escortService.generateMission("1", 0))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-005 [N] Quality > 5 – nem INVALID_QUALITY")
        void generateMission_qualityTooHigh_throws() {
            assertThatThrownBy(() -> escortService.generateMission("1", 6))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-006 [P] Quality hop le – tao va luu nhiem vu moi")
        void generateMission_validQuality_savesAndReturns() {
            given(statsRepository.findByUserId(1L))
                    .willReturn(Optional.of(stats(1L, 0, 10, 3, LocalDateTime.now())));
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.generateMission("1", 3);

            assertThat(result.getQuality()).isEqualTo(3);
            assertThat(result.getStatus()).isEqualTo(0);       // STATUS_AVAILABLE
            assertThat(result.getDistance()).isEqualTo(3000);  // 1000 * quality
        }
    }

    // =========================================================
    // startMission()
    // =========================================================
    @Nested
    @DisplayName("startMission()")
    class StartMission {

        @Test
        @DisplayName("TC-ESCORT-007 [N] Da dat gioi han nhiem vu ngay – nem DAILY_LIMIT_REACHED")
        void startMission_dailyLimitReached_throws() {
            given(statsRepository.findByUserId(1L))
                    .willReturn(Optional.of(stats(1L, 10, 10, 3, LocalDateTime.now())));

            assertThatThrownBy(() -> escortService.startMission("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-008 [N] Da co nhiem vu dang chay – nem ACTIVE_MISSION_EXISTS")
        void startMission_activeMissionExists_throws() {
            given(statsRepository.findByUserId(1L))
                    .willReturn(Optional.of(stats(1L, 0, 10, 3, LocalDateTime.now())));
            given(missionRepository.countByUserIdAndStatus(1L, 1)).willReturn(1L);

            assertThatThrownBy(() -> escortService.startMission("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-009 [N] Nhiem vu khong kha dung – nem MISSION_NOT_AVAILABLE")
        void startMission_missionNotAvailable_throws() {
            given(statsRepository.findByUserId(1L))
                    .willReturn(Optional.of(stats(1L, 0, 10, 3, LocalDateTime.now())));
            given(missionRepository.countByUserIdAndStatus(1L, 1)).willReturn(0L);
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 1, 500, 2000))); // IN_PROGRESS

            assertThatThrownBy(() -> escortService.startMission("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-010 [P] Bat dau nhiem vu chat luong thap – thanh cong, khong tru vang")
        void startMission_lowQuality_success() {
            given(statsRepository.findByUserId(1L))
                    .willReturn(Optional.of(stats(1L, 0, 10, 3, LocalDateTime.now())));
            given(missionRepository.countByUserIdAndStatus(1L, 1)).willReturn(0L);
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 0, 0, 2000))); // AVAILABLE, quality=2
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.startMission("1", 10L);

            assertThat(result.getStatus()).isEqualTo(1); // IN_PROGRESS
            assertThat(result.getStartTime()).isNotNull();
            then(walletClient).should(never()).consumeCurrency(any(), any());
        }

        @Test
        @DisplayName("TC-ESCORT-011 [P] Bat dau nhiem vu chat luong cao (>=4) – tru vang va thanh cong")
        void startMission_highQuality_consumesGoldAndSucceeds() {
            given(statsRepository.findByUserId(1L))
                    .willReturn(Optional.of(stats(1L, 0, 10, 3, LocalDateTime.now())));
            given(missionRepository.countByUserIdAndStatus(1L, 1)).willReturn(0L);
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 4, 0, 0, 4000))); // AVAILABLE, quality=4
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.startMission("1", 10L);

            assertThat(result.getStatus()).isEqualTo(1); // IN_PROGRESS
            then(walletClient).should().consumeCurrency(anyString(), any());
        }
    }

    // =========================================================
    // updateProgress()
    // =========================================================
    @Nested
    @DisplayName("updateProgress()")
    class UpdateProgress {

        @Test
        @DisplayName("TC-ESCORT-012 [N] Nhiem vu khong dang thuc hien – nem MISSION_NOT_IN_PROGRESS")
        void updateProgress_notInProgress_throws() {
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 0, 0, 2000))); // AVAILABLE

            assertThatThrownBy(() -> escortService.updateProgress("1", 10L, 100))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-013 [P] Cap nhat tien do thanh cong – cong don tien do")
        void updateProgress_success_addsProgress() {
            EscortMission m = mission(1L, 10L, 2, 1, 500, 2000); // IN_PROGRESS
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));
            given(missionRepository.findExpiredMissions(eq(1L), any())).willReturn(List.of());
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.updateProgress("1", 10L, 200);

            assertThat(result.getProgress()).isEqualTo(700);
        }

        @Test
        @DisplayName("TC-ESCORT-014 [P] Tien do vuot khoang cach toi da – gioi han o distance")
        void updateProgress_exceedsDistance_cappedAtDistance() {
            EscortMission m = mission(1L, 10L, 2, 1, 1900, 2000); // IN_PROGRESS, near end
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));
            given(missionRepository.findExpiredMissions(eq(1L), any())).willReturn(List.of());
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.updateProgress("1", 10L, 500);

            assertThat(result.getProgress()).isEqualTo(2000); // capped at distance
        }
    }

    // =========================================================
    // completeMission()
    // =========================================================
    @Nested
    @DisplayName("completeMission()")
    class CompleteMission {

        @Test
        @DisplayName("TC-ESCORT-015 [N] Nhiem vu khong dang thuc hien – nem MISSION_NOT_IN_PROGRESS")
        void completeMission_notInProgress_throws() {
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 0, 0, 2000))); // AVAILABLE

            assertThatThrownBy(() -> escortService.completeMission("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-016 [N] Chua hoan thanh khoang cach – nem MISSION_NOT_COMPLETED")
        void completeMission_notEnoughProgress_throws() {
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 1, 500, 2000))); // IN_PROGRESS

            assertThatThrownBy(() -> escortService.completeMission("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-017 [P] Hoan thanh nhiem vu – cap nhat trang thai va thong ke")
        void completeMission_success_updatesStatus() {
            EscortMission m = mission(1L, 10L, 2, 1, 2000, 2000); // progress == distance
            EscortStats s = stats(1L, 0, 10, 3, LocalDateTime.now());
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));
            given(statsRepository.findByUserId(1L)).willReturn(Optional.of(s));
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(statsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.completeMission("1", 10L);

            assertThat(result.getStatus()).isEqualTo(2); // COMPLETED
            assertThat(result.getCompletionTime()).isNotNull();
            assertThat(s.getDailyCompleted()).isEqualTo(1);
            assertThat(s.getTotalCompleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-ESCORT-018 [P] Hoan thanh hoan hao (0 cuoc tan cong) – nhan bonus 1.5x")
        void completeMission_perfect_appliesBonusMultiplier() {
            EscortMission m = mission(1L, 10L, 2, 1, 2000, 2000);
            m.setBonusMultiplier(1.4);
            EscortStats s = stats(1L, 0, 10, 3, LocalDateTime.now());
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));
            given(statsRepository.findByUserId(1L)).willReturn(Optional.of(s));
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(statsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.completeMission("1", 10L);

            assertThat(result.getBonusMultiplier()).isCloseTo(1.4 * 1.5, within(0.001));
            assertThat(s.getPerfectCompletions()).isEqualTo(1);
        }
    }

    // =========================================================
    // failMission()
    // =========================================================
    @Nested
    @DisplayName("failMission()")
    class FailMission {

        @Test
        @DisplayName("TC-ESCORT-019 [N] Nhiem vu khong dang thuc hien – nem MISSION_NOT_IN_PROGRESS")
        void failMission_notInProgress_throws() {
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 0, 0, 2000)));

            assertThatThrownBy(() -> escortService.failMission("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-020 [P] That bai nhiem vu – cap nhat trang thai FAILED va thong ke")
        void failMission_success_setsFailedStatus() {
            EscortMission m = mission(1L, 10L, 2, 1, 500, 2000); // IN_PROGRESS
            EscortStats s = stats(1L, 0, 10, 3, LocalDateTime.now());
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));
            given(statsRepository.findByUserId(1L)).willReturn(Optional.of(s));
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(statsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.failMission("1", 10L);

            assertThat(result.getStatus()).isEqualTo(3); // FAILED
            assertThat(s.getTotalFailed()).isEqualTo(1);
        }
    }

    // =========================================================
    // cancelMission()
    // =========================================================
    @Nested
    @DisplayName("cancelMission()")
    class CancelMission {

        @Test
        @DisplayName("TC-ESCORT-021 [N] Nhiem vu da hoan thanh – nem CANNOT_CANCEL")
        void cancelMission_completed_throws() {
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 2, 2000, 2000))); // COMPLETED

            assertThatThrownBy(() -> escortService.cancelMission("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-022 [P] Huy nhiem vu kha dung – xoa nhiem vu")
        void cancelMission_available_deletesMission() {
            EscortMission m = mission(1L, 10L, 2, 0, 0, 2000); // AVAILABLE
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));

            assertThatNoException().isThrownBy(() -> escortService.cancelMission("1", 10L));
            then(missionRepository).should().delete(m);
        }
    }

    // =========================================================
    // claimReward()
    // =========================================================
    @Nested
    @DisplayName("claimReward()")
    class ClaimReward {

        @Test
        @DisplayName("TC-ESCORT-023 [N] Nhiem vu chua hoan thanh – nem MISSION_NOT_COMPLETED")
        void claimReward_notCompleted_throws() {
            given(missionRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(mission(1L, 10L, 2, 1, 500, 2000))); // IN_PROGRESS

            assertThatThrownBy(() -> escortService.claimReward("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-024 [N] Phan thuong da duoc nhan – nem REWARD_ALREADY_CLAIMED")
        void claimReward_alreadyClaimed_throws() {
            EscortMission m = mission(1L, 10L, 2, 2, 2000, 2000); // COMPLETED
            m.setIsRewardClaimed(true);
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));

            assertThatThrownBy(() -> escortService.claimReward("1", 10L))
                    .isInstanceOf(EscortServiceException.class);
        }

        @Test
        @DisplayName("TC-ESCORT-025 [P] Nhan phan thuong – cap nhat co nhan va cong xu va kinh nghiem")
        void claimReward_success_grantsRewardsAndMarksClaimed() {
            EscortMission m = mission(1L, 10L, 2, 2, 2000, 2000); // COMPLETED, not claimed
            m.setBonusMultiplier(1.0);
            EscortStats s = stats(1L, 0, 10, 3, LocalDateTime.now());
            given(missionRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.of(m));
            given(statsRepository.findByUserId(1L)).willReturn(Optional.of(s));
            given(missionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(statsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortMission result = escortService.claimReward("1", 10L);

            assertThat(result.getIsRewardClaimed()).isTrue();
            then(walletClient).should(times(2)).addCurrency(anyString(), any()); // gold + exp
        }
    }

    // =========================================================
    // getStats()
    // =========================================================
    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("TC-ESCORT-026 [P] Tim thay thong ke – tra ve stats")
        void getStats_found_returnsStats() {
            EscortStats s = stats(1L, 2, 10, 3, LocalDateTime.now());
            given(statsRepository.findByUserId(1L)).willReturn(Optional.of(s));

            assertThat(escortService.getStats("1")).isEqualTo(s);
        }

        @Test
        @DisplayName("TC-ESCORT-027 [N] Khong tim thay thong ke – nem STATS_NOT_FOUND")
        void getStats_notFound_throws() {
            given(statsRepository.findByUserId(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> escortService.getStats("1"))
                    .isInstanceOf(EscortServiceException.class);
        }
    }

    // =========================================================
    // initializeStats()
    // =========================================================
    @Nested
    @DisplayName("initializeStats()")
    class InitializeStats {

        @Test
        @DisplayName("TC-ESCORT-028 [P] Thong ke chua ton tai – tao moi voi gia tri mac dinh")
        void initializeStats_new_savesDefaults() {
            given(statsRepository.existsByUserId(1L)).willReturn(false);
            given(statsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            EscortStats result = escortService.initializeStats("1");

            assertThat(result.getDailyLimit()).isEqualTo(10);
            assertThat(result.getFreeRefreshes()).isEqualTo(3);
            assertThat(result.getDailyCompleted()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-ESCORT-029 [P] Thong ke da ton tai – tra ve thong ke hien tai")
        void initializeStats_alreadyExists_returnsExisting() {
            EscortStats s = stats(1L, 2, 10, 1, LocalDateTime.now());
            given(statsRepository.existsByUserId(1L)).willReturn(true);
            given(statsRepository.findByUserId(1L)).willReturn(Optional.of(s));

            assertThat(escortService.initializeStats("1")).isEqualTo(s);
        }
    }

    // =========================================================
    // hasActiveMission()
    // =========================================================
    @Nested
    @DisplayName("hasActiveMission()")
    class HasActiveMission {

        @Test
        @DisplayName("TC-ESCORT-030 [P] Co nhiem vu dang chay – tra ve true")
        void hasActiveMission_activeExists_returnsTrue() {
            given(missionRepository.countByUserIdAndStatus(1L, 1)).willReturn(1L);

            assertThat(escortService.hasActiveMission("1")).isTrue();
        }

        @Test
        @DisplayName("TC-ESCORT-031 [N] Khong co nhiem vu dang chay – tra ve false")
        void hasActiveMission_noneActive_returnsFalse() {
            given(missionRepository.countByUserIdAndStatus(1L, 1)).willReturn(0L);

            assertThat(escortService.hasActiveMission("1")).isFalse();
        }
    }

    // =========================================================
    // checkExpiredMissions()
    // =========================================================
    @Nested
    @DisplayName("checkExpiredMissions()")
    class CheckExpiredMissions {

        @Test
        @DisplayName("TC-ESCORT-032 [P] Co nhiem vu het han – cap nhat trang thai EXPIRED")
        void checkExpiredMissions_someExpired_setsExpiredStatus() {
            EscortMission m = mission(1L, 10L, 2, 1, 500, 2000); // IN_PROGRESS
            given(missionRepository.findExpiredMissions(eq(1L), any())).willReturn(List.of(m));

            escortService.checkExpiredMissions("1");

            assertThat(m.getStatus()).isEqualTo(4); // EXPIRED
            then(missionRepository).should().saveAll(List.of(m));
        }

        @Test
        @DisplayName("TC-ESCORT-033 [P] Khong co nhiem vu het han – khong luu")
        void checkExpiredMissions_noExpired_noSave() {
            given(missionRepository.findExpiredMissions(eq(1L), any())).willReturn(List.of());

            escortService.checkExpiredMissions("1");

            then(missionRepository).should(never()).saveAll(any());
        }
    }

    // =========================================================
    // getActiveMissions() / getCompletedMissions()
    // =========================================================
    @Nested
    @DisplayName("getActiveMissions() / getCompletedMissions()")
    class QueryMissions {

        @Test
        @DisplayName("TC-ESCORT-034 [P] Lay nhiem vu dang thuc hien")
        void getActiveMissions_returnsInProgressMissions() {
            given(missionRepository.findByUserIdAndStatus(1L, 1))
                    .willReturn(List.of(mission(1L, 10L, 2, 1, 500, 2000)));

            assertThat(escortService.getActiveMissions("1")).hasSize(1);
        }

        @Test
        @DisplayName("TC-ESCORT-035 [P] Lay nhiem vu da hoan thanh")
        void getCompletedMissions_returnsCompletedMissions() {
            given(missionRepository.findByUserIdAndStatus(1L, 2))
                    .willReturn(List.of(mission(1L, 11L, 3, 2, 3000, 3000)));

            assertThat(escortService.getCompletedMissions("1")).hasSize(1);
        }
    }
}
