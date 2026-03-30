package com.SouthMillion.anti_cheat_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.anti_cheat_service.entity.CheatReport;
import com.SouthMillion.anti_cheat_service.entity.PlayerBehavior;
import com.SouthMillion.anti_cheat_service.entity.SuspiciousActivity;
import com.SouthMillion.anti_cheat_service.repository.CheatReportRepository;
import com.SouthMillion.anti_cheat_service.repository.PlayerBehaviorRepository;
import com.SouthMillion.anti_cheat_service.repository.SuspiciousActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("AntiCheatService Tests")
class AntiCheatServiceTest {

    @Mock private CheatReportRepository        cheatReportRepository;
    @Mock private PlayerBehaviorRepository     playerBehaviorRepository;
    @Mock private SuspiciousActivityRepository suspiciousActivityRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private ObjectMapper                 objectMapper;

    @InjectMocks private AntiCheatService antiCheatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(antiCheatService, "behaviorWindow", 300);
        ReflectionTestUtils.setField(antiCheatService, "suspiciousScoreThreshold", 100);
        ReflectionTestUtils.setField(antiCheatService, "autoBanScore", 200);
        ReflectionTestUtils.setField(antiCheatService, "damageHackThreshold", 2.0);
        ReflectionTestUtils.setField(antiCheatService, "resourceGainThreshold", 10.0);
        ReflectionTestUtils.setField(antiCheatService, "speedHackThreshold", 1.5);
        ReflectionTestUtils.setField(antiCheatService, "teleportDistance", 100.0);
    }

    // =========================================================
    // reviewCheatReport
    // =========================================================
    @Nested
    @DisplayName("reviewCheatReport()")
    class ReviewCheatReport {

        @Test
        @DisplayName("TC-AC-001 [P] GM xem xet bao cao – cap nhat trang thai thanh cong")
        void reviewCheatReport_success_updatesStatus() {
            CheatReport report = new CheatReport();
            report.setId(1L);
            report.setUserId("user-010");
            report.setCheatType("SPEED_HACK");
            report.setStatus("DETECTED");
            given(cheatReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(cheatReportRepository.save(any(CheatReport.class))).willAnswer(inv -> inv.getArgument(0));

            CheatReport result = antiCheatService.reviewCheatReport(1L, 99L, "CONFIRMED", "TEMPORARY_BAN", "Confirmed speed hack");

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            assertThat(result.getActionTaken()).isEqualTo("TEMPORARY_BAN");
            assertThat(result.getReviewedBy()).isEqualTo(99L);
            assertThat(result.getNotes()).isEqualTo("Confirmed speed hack");
        }

        @Test
        @DisplayName("TC-AC-002 [N] Bao cao khong ton tai – nem IllegalArgumentException")
        void reviewCheatReport_notFound_throwsException() {
            given(cheatReportRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> antiCheatService.reviewCheatReport(999L, 1L, "CONFIRMED", "BAN", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Report not found");
        }

        @Test
        @DisplayName("TC-AC-003 [P] GM danh dau la false positive")
        void reviewCheatReport_falsePositive_updatesStatus() {
            CheatReport report = new CheatReport();
            report.setId(2L);
            report.setStatus("DETECTED");
            given(cheatReportRepository.findById(2L)).willReturn(Optional.of(report));
            given(cheatReportRepository.save(any(CheatReport.class))).willAnswer(inv -> inv.getArgument(0));

            CheatReport result = antiCheatService.reviewCheatReport(2L, 1L, "false_positive", "none", "Lag issue");

            assertThat(result.getStatus()).isEqualTo("FALSE_POSITIVE");
            assertThat(result.getActionTaken()).isEqualTo("NONE");
        }
    }

    // =========================================================
    // recordBehavior
    // =========================================================
    @Nested
    @DisplayName("recordBehavior()")
    class RecordBehavior {

        @Test
        @DisplayName("TC-AC-004 [P] Hanh vi binh thuong – luu behavior, khong cap nhat Redis")
        void recordBehavior_normal_savesWithoutRedis() {
            given(playerBehaviorRepository.save(any(PlayerBehavior.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // value == expectedValue → deviation = 0 → not anomaly
            antiCheatService.recordBehavior("user-010", "MOVEMENT_SPEED", 5.0, 5.0);

            then(playerBehaviorRepository).should().save(argThat(b ->
                    !b.getIsAnomaly() && b.getDeviation() == 0.0));
            then(redisTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-AC-005 [N] Hanh vi bat thuong (deviation > 0.5) – cap nhat suspicion score")
        void recordBehavior_anomaly_incrementsRedisScore() {
            given(playerBehaviorRepository.save(any(PlayerBehavior.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("10"); // score < suspiciousScoreThreshold(100)

            // value = 200, expected = 100 → deviation = 1.0 → isAnomaly
            antiCheatService.recordBehavior("user-010", "MOVEMENT_SPEED", 200.0, 100.0);

            then(playerBehaviorRepository).should().save(argThat(PlayerBehavior::getIsAnomaly));
            then(valueOps).should().increment(contains("suspicion:score:user-010"), anyLong());
        }

        @Test
        @DisplayName("TC-AC-006 [N] Suspicion score vuot nguong – tao suspicious activity")
        void recordBehavior_highSuspicion_createsSuspiciousActivity() throws Exception {
            given(playerBehaviorRepository.save(any(PlayerBehavior.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("100"); // == suspiciousScoreThreshold(100)
            given(suspiciousActivityRepository.save(any(SuspiciousActivity.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            antiCheatService.recordBehavior("user-010", "DAMAGE_DEALT", 300.0, 100.0);

            then(suspiciousActivityRepository).should().save(argThat(sa ->
                    "HIGH_SUSPICION_SCORE".equals(sa.getActivityType())));
        }
    }

    // =========================================================
    // getUserCheatHistory / getPendingReports / getHighSeverityReports
    // =========================================================
    @Nested
    @DisplayName("getUserCheatHistory() / getPendingReports()")
    class Queries {

        @Test
        @DisplayName("TC-AC-007 [P] Lay lich su cheat cua nguoi dung")
        void getUserCheatHistory_returnsList() {
            CheatReport r = new CheatReport();
            r.setUserId("user-010");
            r.setCheatType("SPEED_HACK");
            given(cheatReportRepository.findByUserIdOrderByCreatedAtDesc("user-010")).willReturn(List.of(r));

            List<CheatReport> result = antiCheatService.getUserCheatHistory("user-010");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCheatType()).isEqualTo("SPEED_HACK");
        }

        @Test
        @DisplayName("TC-AC-008 [P] Lay bao cao cho xu ly (DETECTED)")
        void getPendingReports_returnsList() {
            given(cheatReportRepository.findByStatusOrderByCreatedAtDesc("DETECTED"))
                    .willReturn(List.of(new CheatReport()));

            List<CheatReport> result = antiCheatService.getPendingReports();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-AC-009 [P] Lay bao cao do nghiem trong cao")
        void getHighSeverityReports_returnsList() {
            CheatReport r = new CheatReport();
            r.setSeverity(4);
            given(cheatReportRepository.findHighSeverityUnreviewedReports(3)).willReturn(List.of(r));

            List<CheatReport> result = antiCheatService.getHighSeverityReports(3);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSeverity()).isEqualTo(4);
        }
    }

    // =========================================================
    // getSuspiciousActivities / getUnresolvedActivities
    // =========================================================
    @Nested
    @DisplayName("getSuspiciousActivities() / getUnresolvedActivities()")
    class SuspiciousActivityQueries {

        @Test
        @DisplayName("TC-AC-010 [P] Lay hoat dong kha nghi cua nguoi dung")
        void getSuspiciousActivities_returnsList() {
            SuspiciousActivity sa = new SuspiciousActivity();
            sa.setUserId("user-010");
            sa.setActivityType("HIGH_SUSPICION_SCORE");
            given(suspiciousActivityRepository.findByUserIdOrderByCreatedAtDesc("user-010"))
                    .willReturn(List.of(sa));

            List<SuspiciousActivity> result = antiCheatService.getSuspiciousActivities("user-010");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-AC-011 [P] Lay cac hoat dong chua giai quyet")
        void getUnresolvedActivities_returnsList() {
            SuspiciousActivity sa = new SuspiciousActivity();
            sa.setIsResolved(false);
            given(suspiciousActivityRepository.findByIsResolvedOrderByCreatedAtDesc(false))
                    .willReturn(List.of(sa));

            List<SuspiciousActivity> result = antiCheatService.getUnresolvedActivities();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsResolved()).isFalse();
        }
    }

    // =========================================================
    // reportDamage
    // =========================================================
    @Nested
    @DisplayName("reportDamage()")
    class ReportDamage {

        @Test
        @DisplayName("TC-AC-012 [P] Sát thương binh thuong – chi ghi behavior")
        void reportDamage_normalDamage_recordsBehaviorOnly() {
            given(playerBehaviorRepository.save(any(PlayerBehavior.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // damage = 100, expected = 100, threshold = 2.0 → 100 <= 100*2 → no cheat report
            antiCheatService.reportDamage("user-010", "user-020", 100.0, 100.0);

            then(playerBehaviorRepository).should().save(any(PlayerBehavior.class));
            then(cheatReportRepository).shouldHaveNoInteractions();
        }
    }
}
