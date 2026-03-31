package com.game.trial.service.impl;

import com.SouthMillion.trial_service.client.BagClient;
import com.SouthMillion.trial_service.client.WalletClient;
import com.SouthMillion.trial_service.exception.TrialServiceException;
import com.SouthMillion.trial_service.model.entity.TrialRecord;
import com.SouthMillion.trial_service.repository.TrialRecordRepository;
import com.SouthMillion.trial_service.service.impl.TrialServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrialServiceImpl Tests")
class TrialServiceImplTest {

    @Mock private TrialRecordRepository trialRecordRepository;
    @Mock private BagClient             bagClient;
    @Mock private WalletClient          walletClient;

    @InjectMocks private TrialServiceImpl trialService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Creates a trial record with typical in-progress=false state. */
    private static TrialRecord record(Long userId, Integer trialId,
                                       boolean inProgress, int remaining, int bestStage) {
        TrialRecord r = new TrialRecord();
        r.setId(1L);
        r.setUserId(userId);
        r.setTrialId(trialId);
        r.setCurrentStage(1);
        r.setBestStage(bestStage);
        r.setTotalAttempts(0);
        r.setRemainingAttempts(remaining);
        r.setIsInProgress(inProgress);
        r.setCurrentScore(0L);
        r.setBestScore(0L);
        r.setStars(0);
        r.setLastResetDate(LocalDateTime.now()); // reset today
        r.setRewardsClaimed(0L);
        return r;
    }

    private static TrialRecord recordWithStaleReset(Long userId, Integer trialId) {
        TrialRecord r = record(userId, trialId, false, 0, 5);
        r.setLastResetDate(LocalDate.now().minusDays(1).atStartOfDay()); // yesterday
        return r;
    }

    // =========================================================
    // getAllRecords()
    // =========================================================
    @Nested
    @DisplayName("getAllRecords()")
    class GetAllRecords {

        @Test
        @DisplayName("TC-TRIAL-001 [P] Tra ve tat ca ban ghi trial cua nguoi dung")
        void getAllRecords_returnsAll() {
            List<TrialRecord> records = List.of(record(1L, 1, false, 3, 0));
            given(trialRecordRepository.findByUserId(1L)).willReturn(records);

            List<TrialRecord> result = trialService.getAllRecords(1L);

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================
    // getRecord()
    // =========================================================
    @Nested
    @DisplayName("getRecord()")
    class GetRecord {

        @Test
        @DisplayName("TC-TRIAL-002 [P] Tim thay ban ghi – tra ve entity")
        void getRecord_found_returnsEntity() {
            TrialRecord r = record(1L, 1, false, 3, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            TrialRecord result = trialService.getRecord(1L, 1);

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getTrialId()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-TRIAL-003 [N] Khong tim thay – nem TrialServiceException TRIAL_RECORD_NOT_FOUND")
        void getRecord_notFound_throws() {
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> trialService.getRecord(1L, 99))
                    .isInstanceOf(TrialServiceException.class)
                    .hasMessageContaining("not found")
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("TRIAL_RECORD_NOT_FOUND");
        }
    }

    // =========================================================
    // initializeRecord()
    // =========================================================
    @Nested
    @DisplayName("initializeRecord()")
    class InitializeRecord {

        @Test
        @DisplayName("TC-TRIAL-004 [P] Record chua ton tai – tao moi voi gia tri mac dinh")
        void initializeRecord_notExists_createsNew() {
            given(trialRecordRepository.existsByUserIdAndTrialId(1L, 1)).willReturn(false);
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TrialRecord result = trialService.initializeRecord(1L, 1);

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getTrialId()).isEqualTo(1);
            assertThat(result.getCurrentStage()).isEqualTo(1);
            assertThat(result.getBestStage()).isEqualTo(0);
            assertThat(result.getRemainingAttempts()).isEqualTo(3);
            assertThat(result.getIsInProgress()).isFalse();
            assertThat(result.getRewardsClaimed()).isEqualTo(0L);
        }

        @Test
        @DisplayName("TC-TRIAL-005 [P] Record da ton tai – tra ve record hien tai")
        void initializeRecord_alreadyExists_returnsExisting() {
            TrialRecord existing = record(1L, 1, false, 2, 3);
            given(trialRecordRepository.existsByUserIdAndTrialId(1L, 1)).willReturn(true);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(existing));

            TrialRecord result = trialService.initializeRecord(1L, 1);

            assertThat(result.getRemainingAttempts()).isEqualTo(2); // existing state preserved
            then(trialRecordRepository).should(never()).save(any());
        }
    }

    // =========================================================
    // startTrial()
    // =========================================================
    @Nested
    @DisplayName("startTrial()")
    class StartTrial {

        @Test
        @DisplayName("TC-TRIAL-006 [P] Trial hop le – bat dau va giam remaining attempts")
        void startTrial_valid_decrementsAttempts() {
            TrialRecord r = record(1L, 1, false, 3, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TrialRecord result = trialService.startTrial(1L, 1);

            assertThat(result.getIsInProgress()).isTrue();
            assertThat(result.getRemainingAttempts()).isEqualTo(2);
            assertThat(result.getTotalAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-TRIAL-007 [P] Trial moi (record chua ton tai) – khoi tao va bat dau")
        void startTrial_newRecord_initializesAndStarts() {
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 2)).willReturn(Optional.empty());
            given(trialRecordRepository.existsByUserIdAndTrialId(1L, 2)).willReturn(false);
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TrialRecord result = trialService.startTrial(1L, 2);

            assertThat(result.getIsInProgress()).isTrue();
        }

        @Test
        @DisplayName("TC-TRIAL-008 [P] Trial cap cao (trialId >= 10) – tru gold entry cost")
        void startTrial_highLevelTrial_consumesGold() {
            TrialRecord r = record(1L, 10, false, 3, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 10)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            trialService.startTrial(1L, 10);

            // Entry cost = (10 - 9) * 100 = 100 gold
            then(walletClient).should().consumeCurrency(eq("1"), any());
        }

        @Test
        @DisplayName("TC-TRIAL-009 [P] Reset hang ngay can thiet – tu dong reset truoc khi bat dau")
        void startTrial_needsDailyReset_resetsAttempts() {
            TrialRecord stale = recordWithStaleReset(1L, 1);
            TrialRecord afterReset = record(1L, 1, false, 3, 0);
            // First call returns stale record, after reset call returns fresh record
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1))
                    .willReturn(Optional.of(stale))
                    .willReturn(Optional.of(afterReset));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TrialRecord result = trialService.startTrial(1L, 1);

            assertThat(result.getIsInProgress()).isTrue();
        }

        @Test
        @DisplayName("TC-TRIAL-010 [N] Trial dang in-progress – nem TRIAL_IN_PROGRESS")
        void startTrial_alreadyInProgress_throws() {
            TrialRecord r = record(1L, 1, true, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.startTrial(1L, 1))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("TRIAL_IN_PROGRESS");
        }

        @Test
        @DisplayName("TC-TRIAL-011 [N] Het luot choi – nem NO_ATTEMPTS_REMAINING")
        void startTrial_noAttempts_throws() {
            TrialRecord r = record(1L, 1, false, 0, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.startTrial(1L, 1))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("NO_ATTEMPTS_REMAINING");
        }
    }

    // =========================================================
    // completeTrial()
    // =========================================================
    @Nested
    @DisplayName("completeTrial()")
    class CompleteTrial {

        @Test
        @DisplayName("TC-TRIAL-012 [P] Hoan thanh 3 sao – cap nhat record va phat thuong gold + item")
        void completeTrial_threeStars_awardsGoldAndItem() {
            TrialRecord r = record(1L, 1, true, 2, 0);
            r.setBestScore(0L);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TrialRecord result = trialService.completeTrial(1L, 1, 1000L, 3, 120);

            assertThat(result.getIsInProgress()).isFalse();
            assertThat(result.getCurrentScore()).isEqualTo(1000L);
            assertThat(result.getStars()).isEqualTo(3);
            // goldReward = 500 * 3 * 1 = 1500
            then(walletClient).should().addCurrency(eq("1"), any());
            // stars >= 2 → items awarded
            then(bagClient).should().grantItems(any());
        }

        @Test
        @DisplayName("TC-TRIAL-013 [P] Hoan thanh 1 sao – cap nhat record, chi phat gold, khong phat item")
        void completeTrial_oneStar_awardsGoldOnly() {
            TrialRecord r = record(1L, 1, true, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            trialService.completeTrial(1L, 1, 100L, 1, null);

            then(walletClient).should().addCurrency(eq("1"), any());
            then(bagClient).should(never()).grantItems(any());
        }

        @Test
        @DisplayName("TC-TRIAL-014 [P] Hoan thanh 0 sao – khong phat thuong")
        void completeTrial_zeroStars_noRewards() {
            TrialRecord r = record(1L, 1, true, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            trialService.completeTrial(1L, 1, 0L, 0, null);

            then(walletClient).should(never()).addCurrency(any(), any());
            then(bagClient).should(never()).grantItems(any());
        }

        @Test
        @DisplayName("TC-TRIAL-015 [N] Trial khong dang in-progress – nem TRIAL_NOT_IN_PROGRESS")
        void completeTrial_notInProgress_throws() {
            TrialRecord r = record(1L, 1, false, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.completeTrial(1L, 1, 100L, 3, 120))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("TRIAL_NOT_IN_PROGRESS");
        }

        @Test
        @DisplayName("TC-TRIAL-016 [N] So sao khong hop le (> 3) – nem INVALID_STARS")
        void completeTrial_invalidStars_throws() {
            TrialRecord r = record(1L, 1, true, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.completeTrial(1L, 1, 100L, 4, 120))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("INVALID_STARS");
        }

        @Test
        @DisplayName("TC-TRIAL-017 [N] So sao am – nem INVALID_STARS")
        void completeTrial_negativeStars_throws() {
            TrialRecord r = record(1L, 1, true, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.completeTrial(1L, 1, 100L, -1, 120))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("INVALID_STARS");
        }
    }

    // =========================================================
    // failTrial()
    // =========================================================
    @Nested
    @DisplayName("failTrial()")
    class FailTrial {

        @Test
        @DisplayName("TC-TRIAL-018 [P] Trial that bai – reset score va ket thuc in-progress")
        void failTrial_valid_resetsScoreAndEndsProgress() {
            TrialRecord r = record(1L, 1, true, 2, 0);
            r.setCurrentScore(500L);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TrialRecord result = trialService.failTrial(1L, 1);

            assertThat(result.getIsInProgress()).isFalse();
            assertThat(result.getCurrentScore()).isEqualTo(0L);
            assertThat(result.getCompletionTime()).isNull();
        }

        @Test
        @DisplayName("TC-TRIAL-019 [N] Trial khong dang in-progress – nem TRIAL_NOT_IN_PROGRESS")
        void failTrial_notInProgress_throws() {
            TrialRecord r = record(1L, 1, false, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.failTrial(1L, 1))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("TRIAL_NOT_IN_PROGRESS");
        }
    }

    // =========================================================
    // advanceStage()
    // =========================================================
    @Nested
    @DisplayName("advanceStage()")
    class AdvanceStage {

        @Test
        @DisplayName("TC-TRIAL-020 [P] Tang stage – currentStage tang 1")
        void advanceStage_valid_incrementsStage() {
            TrialRecord r = record(1L, 1, true, 2, 5);
            r.setCurrentStage(5);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TrialRecord result = trialService.advanceStage(1L, 1);

            assertThat(result.getCurrentStage()).isEqualTo(6);
            assertThat(result.getBestStage()).isEqualTo(6); // updated best stage
        }

        @Test
        @DisplayName("TC-TRIAL-021 [N] Da o stage toi da (100) – nem MAX_STAGE_REACHED")
        void advanceStage_maxStage_throws() {
            TrialRecord r = record(1L, 1, true, 2, 100);
            r.setCurrentStage(100);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.advanceStage(1L, 1))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("MAX_STAGE_REACHED");
        }

        @Test
        @DisplayName("TC-TRIAL-022 [N] Trial khong dang in-progress – nem TRIAL_NOT_IN_PROGRESS")
        void advanceStage_notInProgress_throws() {
            TrialRecord r = record(1L, 1, false, 2, 5);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.advanceStage(1L, 1))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("TRIAL_NOT_IN_PROGRESS");
        }
    }

    // =========================================================
    // resetDailyAttempts()
    // =========================================================
    @Nested
    @DisplayName("resetDailyAttempts()")
    class ResetDailyAttempts {

        @Test
        @DisplayName("TC-TRIAL-023 [P] Reset luot choi hang ngay – dat lai 3 luot")
        void resetDailyAttempts_setsToDefault() {
            TrialRecord r = record(1L, 1, false, 0, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            trialService.resetDailyAttempts(1L, 1);

            then(trialRecordRepository).should().save(argThat(rec ->
                    rec.getRemainingAttempts() == 3));
        }
    }

    // =========================================================
    // claimReward()
    // =========================================================
    @Nested
    @DisplayName("claimReward()")
    class ClaimReward {

        @Test
        @DisplayName("TC-TRIAL-024 [P] Nhan thuong stage 1 – phat gold va item, danh dau da nhan")
        void claimReward_stage1_awardsGoldAndItems() {
            TrialRecord r = record(1L, 1, false, 2, 5);
            r.setRewardsClaimed(0L);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            boolean result = trialService.claimReward(1L, 1, 1);

            assertThat(result).isTrue();
            // gold = 1000 * 1 = 1000
            then(walletClient).should().addCurrency(eq("1"), any());
            // item 20002, quantity = 1/5+1 = 1
            then(bagClient).should().grantItems(any());
        }

        @Test
        @DisplayName("TC-TRIAL-025 [P] Nhan thuong stage 5 – phat gold va 2 items")
        void claimReward_stage5_awardsCorrectQuantity() {
            TrialRecord r = record(1L, 1, false, 2, 10);
            r.setRewardsClaimed(0L);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));
            given(trialRecordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            boolean result = trialService.claimReward(1L, 1, 5);

            assertThat(result).isTrue();
            // item quantity = 5/5+1 = 2
            then(bagClient).should().grantItems(argThat(req ->
                    req.getItems().get(0).getNum() == 2));
        }

        @Test
        @DisplayName("TC-TRIAL-026 [N] Stage chua hoan thanh – nem STAGE_NOT_COMPLETED")
        void claimReward_stageNotCompleted_throws() {
            TrialRecord r = record(1L, 1, false, 2, 3); // bestStage=3
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.claimReward(1L, 1, 5)) // stageId=5 > bestStage=3
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("STAGE_NOT_COMPLETED");
        }

        @Test
        @DisplayName("TC-TRIAL-027 [N] Da nhan phan thuong – nem REWARD_ALREADY_CLAIMED")
        void claimReward_alreadyClaimed_throws() {
            TrialRecord r = record(1L, 1, false, 2, 5);
            r.setRewardsClaimed(1L); // bit 0 set = stage 1 claimed
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThatThrownBy(() -> trialService.claimReward(1L, 1, 1))
                    .isInstanceOf(TrialServiceException.class)
                    .extracting(e -> ((TrialServiceException) e).getErrorCode())
                    .isEqualTo("REWARD_ALREADY_CLAIMED");
        }
    }

    // =========================================================
    // isRewardClaimed()
    // =========================================================
    @Nested
    @DisplayName("isRewardClaimed()")
    class IsRewardClaimed {

        @Test
        @DisplayName("TC-TRIAL-028 [P] Stage 1 da nhan – tra ve true")
        void isRewardClaimed_stage1Claimed_returnsTrue() {
            TrialRecord r = record(1L, 1, false, 2, 5);
            r.setRewardsClaimed(1L); // bit 0 set
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThat(trialService.isRewardClaimed(1L, 1, 1)).isTrue();
        }

        @Test
        @DisplayName("TC-TRIAL-029 [P] Stage 2 chua nhan – tra ve false")
        void isRewardClaimed_stage2NotClaimed_returnsFalse() {
            TrialRecord r = record(1L, 1, false, 2, 5);
            r.setRewardsClaimed(1L); // only stage 1 claimed
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThat(trialService.isRewardClaimed(1L, 1, 2)).isFalse();
        }
    }

    // =========================================================
    // getClaimedRewards()
    // =========================================================
    @Nested
    @DisplayName("getClaimedRewards()")
    class GetClaimedRewards {

        @Test
        @DisplayName("TC-TRIAL-030 [P] Stage 1 va 3 da nhan – tra ve [1, 3]")
        void getClaimedRewards_stages1and3_returnsCorrectList() {
            TrialRecord r = record(1L, 1, false, 2, 5);
            r.setRewardsClaimed(0b0101L); // bits 0 and 2 set = stages 1 and 3
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            List<Integer> result = trialService.getClaimedRewards(1L, 1);

            assertThat(result).containsExactly(1, 3);
        }

        @Test
        @DisplayName("TC-TRIAL-031 [P] Chua nhan bat ky phan thuong – tra ve list rong")
        void getClaimedRewards_none_returnsEmpty() {
            TrialRecord r = record(1L, 1, false, 2, 5);
            r.setRewardsClaimed(0L);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThat(trialService.getClaimedRewards(1L, 1)).isEmpty();
        }
    }

    // =========================================================
    // getTotalScore()
    // =========================================================
    @Nested
    @DisplayName("getTotalScore()")
    class GetTotalScore {

        @Test
        @DisplayName("TC-TRIAL-032 [P] Co tong diem – tra ve tong")
        void getTotalScore_hasScore_returnsTotal() {
            given(trialRecordRepository.getTotalScore(1L)).willReturn(5000L);

            assertThat(trialService.getTotalScore(1L)).isEqualTo(5000L);
        }

        @Test
        @DisplayName("TC-TRIAL-033 [P] Repository tra ve null – tra ve 0")
        void getTotalScore_null_returnsZero() {
            given(trialRecordRepository.getTotalScore(1L)).willReturn(null);

            assertThat(trialService.getTotalScore(1L)).isEqualTo(0L);
        }
    }

    // =========================================================
    // hasAttemptsRemaining()
    // =========================================================
    @Nested
    @DisplayName("hasAttemptsRemaining()")
    class HasAttemptsRemaining {

        @Test
        @DisplayName("TC-TRIAL-034 [P] Con luot choi – tra ve true")
        void hasAttemptsRemaining_hasAttempts_returnsTrue() {
            TrialRecord r = record(1L, 1, false, 2, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThat(trialService.hasAttemptsRemaining(1L, 1)).isTrue();
        }

        @Test
        @DisplayName("TC-TRIAL-035 [P] Het luot nhung can daily reset – tra ve true")
        void hasAttemptsRemaining_needsDailyReset_returnsTrue() {
            TrialRecord r = recordWithStaleReset(1L, 1);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThat(trialService.hasAttemptsRemaining(1L, 1)).isTrue();
        }

        @Test
        @DisplayName("TC-TRIAL-036 [N] Het luot choi – tra ve false")
        void hasAttemptsRemaining_noAttempts_returnsFalse() {
            TrialRecord r = record(1L, 1, false, 0, 0);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            assertThat(trialService.hasAttemptsRemaining(1L, 1)).isFalse();
        }

        @Test
        @DisplayName("TC-TRIAL-037 [N] Khong tim thay record – tra ve false")
        void hasAttemptsRemaining_recordNotFound_returnsFalse() {
            given(trialRecordRepository.findByUserIdAndTrialId(99L, 1)).willReturn(Optional.empty());

            assertThat(trialService.hasAttemptsRemaining(99L, 1)).isFalse();
        }
    }

    // =========================================================
    // resetProgress()
    // =========================================================
    @Nested
    @DisplayName("resetProgress()")
    class ResetProgress {

        @Test
        @DisplayName("TC-TRIAL-038 [P] Reset tien do – xoa tat ca du lieu tien trinh")
        void resetProgress_clearsAllProgress() {
            TrialRecord r = record(1L, 1, false, 2, 15);
            r.setCurrentStage(10);
            r.setBestScore(5000L);
            r.setStars(3);
            r.setRewardsClaimed(255L);
            given(trialRecordRepository.findByUserIdAndTrialId(1L, 1)).willReturn(Optional.of(r));

            trialService.resetProgress(1L, 1);

            then(trialRecordRepository).should().save(argThat(rec ->
                    rec.getCurrentStage() == 1 &&
                    rec.getBestStage() == 0 &&
                    rec.getBestScore() == 0L &&
                    rec.getStars() == 0 &&
                    rec.getRewardsClaimed() == 0L &&
                    !rec.getIsInProgress()
            ));
        }
    }
}
