package com.SouthMillion.lingzhu_service.service;

import com.SouthMillion.lingzhu_service.entity.LingZhuProgress;
import com.SouthMillion.lingzhu_service.repository.LingZhuProgressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LingZhuService Tests")
class LingZhuServiceTest {

    @Mock
    private LingZhuProgressRepository repository;

    @InjectMocks
    private LingZhuService lingZhuService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static LingZhuProgress progress(Long roleId, int stage, int passLevel, int sweepCount) {
        return LingZhuProgress.builder()
                .roleId(roleId)
                .stage(stage)
                .passLevel(passLevel)
                .sweepCount(sweepCount)
                .build();
    }

    // =========================================================
    // getAll()
    // =========================================================
    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("TC-LZ-001 [P] Có sẵn dữ liệu – trả về danh sách từ DB")
        void getAll_existingRecords_returnsFromDb() {
            List<LingZhuProgress> dbList = List.of(
                    progress(1L, 1, 3, 2),
                    progress(1L, 2, 1, 0),
                    progress(1L, 3, 0, 0)
            );
            given(repository.findByRoleId(1L)).willReturn(dbList);

            List<LingZhuProgress> result = lingZhuService.getAll(1L);

            assertThat(result).hasSize(3);
            assertThat(result).isEqualTo(dbList);
        }

        @Test
        @DisplayName("TC-LZ-002 [P] Người chơi mới – trả về 3 stage mặc định với passLevel=0, sweepCount=0")
        void getAll_newPlayer_returnsThreeDefaultStages() {
            given(repository.findByRoleId(99L)).willReturn(List.of());

            List<LingZhuProgress> result = lingZhuService.getAll(99L);

            assertThat(result).hasSize(3);
            for (int i = 0; i < 3; i++) {
                LingZhuProgress p = result.get(i);
                assertThat(p.getRoleId()).isEqualTo(99L);
                assertThat(p.getStage()).isEqualTo(i + 1);
                assertThat(p.getPassLevel()).isEqualTo(0);
                assertThat(p.getSweepCount()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("TC-LZ-003 [P] Người chơi mới – stage đánh số đúng 1, 2, 3")
        void getAll_newPlayer_stagesNumberedCorrectly() {
            given(repository.findByRoleId(2L)).willReturn(List.of());

            List<LingZhuProgress> result = lingZhuService.getAll(2L);

            assertThat(result)
                    .extracting(LingZhuProgress::getStage)
                    .containsExactly(1, 2, 3);
        }
    }

    // =========================================================
    // challenge()
    // =========================================================
    @Nested
    @DisplayName("challenge()")
    class Challenge {

        @Test
        @DisplayName("TC-LZ-004 [P] Thử thách lần đầu stage 1, level 1 – thành công và lưu DB")
        void challenge_firstTime_level1_success() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 0, 0)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = lingZhuService.challenge(1L, 1, 1);

            assertThat(result.get("success")).isEqualTo(true);
            verify(repository).save(any(LingZhuProgress.class));
        }

        @Test
        @DisplayName("TC-LZ-005 [P] Thử thách level tiếp theo (passLevel+1) – thành công và lưu")
        void challenge_nextLevel_success() {
            given(repository.findByRoleIdAndStage(1L, 2))
                    .willReturn(Optional.of(progress(1L, 2, 5, 0)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = lingZhuService.challenge(1L, 2, 6);

            assertThat(result.get("success")).isEqualTo(true);
            verify(repository).save(any(LingZhuProgress.class));
        }

        @Test
        @DisplayName("TC-LZ-006 [P] Thử thách lại level đã qua – thành công nhưng không lưu lại")
        void challenge_alreadyPassedLevel_successNoSave() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 3, 0)));

            Map<String, Object> result = lingZhuService.challenge(1L, 1, 2);

            assertThat(result.get("success")).isEqualTo(true);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC-LZ-007 [N] Thử thách bỏ qua level (p1 > passLevel+1) – thất bại")
        void challenge_skipLevel_fails() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 2, 0)));

            Map<String, Object> result = lingZhuService.challenge(1L, 1, 5);

            assertThat(result.get("success")).isEqualTo(false);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC-LZ-008 [N] p1 = 0 – thất bại (level 0 không hợp lệ)")
        void challenge_levelZero_fails() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 0, 0)));

            Map<String, Object> result = lingZhuService.challenge(1L, 1, 0);

            assertThat(result.get("success")).isEqualTo(false);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC-LZ-009 [P] Chưa có progress trong DB – tự tạo mới và thử level 1")
        void challenge_noExistingProgress_createsAndSaves() {
            given(repository.findByRoleIdAndStage(5L, 1)).willReturn(Optional.empty());
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = lingZhuService.challenge(5L, 1, 1);

            assertThat(result.get("success")).isEqualTo(true);
            verify(repository).save(any(LingZhuProgress.class));
        }

        @Test
        @DisplayName("TC-LZ-010 [N] Chưa có progress trong DB – thử level 2 ngay (bỏ qua) – thất bại")
        void challenge_noExistingProgress_skipLevel_fails() {
            given(repository.findByRoleIdAndStage(5L, 1)).willReturn(Optional.empty());

            Map<String, Object> result = lingZhuService.challenge(5L, 1, 2);

            assertThat(result.get("success")).isEqualTo(false);
            verify(repository, never()).save(any());
        }
    }

    // =========================================================
    // sweep()
    // =========================================================
    @Nested
    @DisplayName("sweep()")
    class Sweep {

        @Test
        @DisplayName("TC-LZ-011 [P] Sweep hợp lệ – sweepCount tăng và lưu")
        void sweep_valid_incrementsAndSaves() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 3, 2)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = lingZhuService.sweep(1L, 1, 3);

            assertThat(result.get("success")).isEqualTo(true);
            verify(repository).save(argThat(p -> ((LingZhuProgress) p).getSweepCount() == 5));
        }

        @Test
        @DisplayName("TC-LZ-012 [P] Sweep đúng giới hạn 10 lần – thành công")
        void sweep_exactLimit_success() {
            given(repository.findByRoleIdAndStage(1L, 2))
                    .willReturn(Optional.of(progress(1L, 2, 5, 7)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = lingZhuService.sweep(1L, 2, 3);

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("TC-LZ-013 [N] Sweep vượt giới hạn 10 lần – thất bại")
        void sweep_exceedsLimit_fails() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 3, 8)));

            Map<String, Object> result = lingZhuService.sweep(1L, 1, 3);

            assertThat(result.get("success")).isEqualTo(false);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC-LZ-014 [N] passLevel = 0 (chưa qua level nào) – không được sweep")
        void sweep_passLevelZero_fails() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 0, 0)));

            Map<String, Object> result = lingZhuService.sweep(1L, 1, 1);

            assertThat(result.get("success")).isEqualTo(false);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC-LZ-015 [P] Chưa có progress trong DB – tự tạo mới, passLevel=0 – thất bại sweep")
        void sweep_noExistingProgress_fails() {
            given(repository.findByRoleIdAndStage(5L, 1)).willReturn(Optional.empty());

            Map<String, Object> result = lingZhuService.sweep(5L, 1, 1);

            assertThat(result.get("success")).isEqualTo(false);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC-LZ-016 [P] Sweep 1 lần khi sweepCount=9 – đúng giới hạn, thành công")
        void sweep_oneMoreAtNine_success() {
            given(repository.findByRoleIdAndStage(1L, 3))
                    .willReturn(Optional.of(progress(1L, 3, 2, 9)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = lingZhuService.sweep(1L, 3, 1);

            assertThat(result.get("success")).isEqualTo(true);
        }

        @Test
        @DisplayName("TC-LZ-017 [N] Sweep khi sweepCount đã đủ 10 – thất bại")
        void sweep_alreadyAtLimit_fails() {
            given(repository.findByRoleIdAndStage(1L, 1))
                    .willReturn(Optional.of(progress(1L, 1, 5, 10)));

            Map<String, Object> result = lingZhuService.sweep(1L, 1, 1);

            assertThat(result.get("success")).isEqualTo(false);
            verify(repository, never()).save(any());
        }
    }
}

