package com.SouthMillion.moderation_service.service;

import com.SouthMillion.moderation_service.entity.Report;
import com.SouthMillion.moderation_service.entity.Violation;
import com.SouthMillion.moderation_service.repository.ReportRepository;
import com.SouthMillion.moderation_service.repository.ViolationRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("ModerationService Tests")
class ModerationServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ViolationRepository violationRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(moderationService, "badWordsStr", "bad,damn");
        ReflectionTestUtils.setField(moderationService, "maxViolations", 3);
        ReflectionTestUtils.setField(moderationService, "banDurationHours", 24);
        ReflectionTestUtils.setField(moderationService, "spamThreshold", 5);
        ReflectionTestUtils.setField(moderationService, "spamWindowSeconds", 10);
        // Reset lazy-init badWords cache between tests
        ReflectionTestUtils.setField(moderationService, "badWords", null);
    }

    // =========================================================
    // filterMessage
    // =========================================================
    @Nested
    @DisplayName("filterMessage()")
    class FilterMessage {

        @Test
        @DisplayName("TC-MOD-001 [N] Nguoi dung bi mute – tra ve khong duoc phep")
        void filterMessage_userMuted_returnsNotAllowed() {
            given(redisTemplate.hasKey("muted:user-001")).willReturn(true);

            Map<String, Object> result = moderationService.filterMessage("user-001", "Hello world");

            assertThat(result.get("allowed")).isEqualTo(false);
            assertThat(result.get("reason").toString()).containsIgnoringCase("muted");
            then(violationRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-MOD-002 [N] Spam phat hien – tra ve khong duoc phep + ghi vi pham")
        void filterMessage_spamDetected_returnsNotAllowed() {
            given(redisTemplate.hasKey(anyString())).willReturn(false);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("5"); // count == spamThreshold
            given(violationRepository.countByUserIdAndCreatedAtAfter(eq("user-001"), any()))
                    .willReturn(0L);
            given(violationRepository.save(any(Violation.class))).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = moderationService.filterMessage("user-001", "Fast message");

            assertThat(result.get("allowed")).isEqualTo(false);
            assertThat(result.get("reason").toString()).containsIgnoringCase("spam");
            then(violationRepository).should().save(any(Violation.class));
        }

        @Test
        @DisplayName("TC-MOD-003 [N] Tin nhan chua tu cam – loc va ghi vi pham")
        void filterMessage_badWord_returnsFiltered() {
            given(redisTemplate.hasKey(anyString())).willReturn(false);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("0"); // not spam
            given(violationRepository.countByUserIdAndCreatedAtAfter(eq("user-001"), any()))
                    .willReturn(0L);
            given(violationRepository.save(any(Violation.class))).willAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = moderationService.filterMessage("user-001", "Hello bad world");

            assertThat(result.get("allowed")).isEqualTo(true);
            assertThat(result.get("filtered").toString()).contains("***");
            assertThat(result.get("hasBadWords")).isEqualTo(true);
            then(violationRepository).should().save(any(Violation.class));
        }

        @Test
        @DisplayName("TC-MOD-004 [P] Tin nhan sach – cho phep va khong loc")
        void filterMessage_cleanMessage_returnsAllowed() {
            given(redisTemplate.hasKey(anyString())).willReturn(false);
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("0");

            Map<String, Object> result = moderationService.filterMessage("user-001", "Hello world");

            assertThat(result.get("allowed")).isEqualTo(true);
            assertThat(result.get("filtered")).isEqualTo("Hello world");
            assertThat(result.get("hasBadWords")).isEqualTo(false);
            then(violationRepository).shouldHaveNoInteractions();
        }
    }

    // =========================================================
    // createReport
    // =========================================================
    @Nested
    @DisplayName("createReport()")
    class CreateReport {

        @Test
        @DisplayName("TC-MOD-005 [P] Tao bao cao thanh cong – chua du 5 bao cao cho auto-ban")
        void createReport_success_belowAutoModThreshold() {
            Report saved = new Report();
            saved.setId(1L);
            saved.setReportedUserId("user-002");
            given(reportRepository.save(any(Report.class))).willReturn(saved);
            given(reportRepository.countByReportedUserIdAndStatus(eq("user-002"), eq("PENDING")))
                    .willReturn(4L); // < 5 → no auto-moderate

            Report result = moderationService.createReport("user-001", "user-002", "CHAT_SPAM", "Spam msg", null);

            assertThat(result.getId()).isEqualTo(1L);
            then(violationRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-MOD-006 [N] Bao cao dat 5 – kich hoat auto-moderation")
        void createReport_fiveReports_triggersAutoModerate() {
            Report saved = new Report();
            saved.setId(2L);
            saved.setReportedUserId("user-099");
            given(reportRepository.save(any(Report.class))).willReturn(saved);
            given(reportRepository.countByReportedUserIdAndStatus(eq("user-099"), eq("PENDING")))
                    .willReturn(5L); // >= 5 → autoModerate
            // autoModerate → recordViolation → violationRepository
            given(violationRepository.countByUserIdAndCreatedAtAfter(eq("user-099"), any()))
                    .willReturn(0L);
            given(violationRepository.save(any(Violation.class))).willAnswer(inv -> inv.getArgument(0));
            // autoModerate also calls banUser → redisTemplate
            given(redisTemplate.opsForValue()).willReturn(valueOps);

            moderationService.createReport("user-001", "user-099", "CHEATING", "Evidence", null);

            then(violationRepository).should().save(any(Violation.class));
        }
    }

    // =========================================================
    // handleReport
    // =========================================================
    @Nested
    @DisplayName("handleReport()")
    class HandleReport {

        @Test
        @DisplayName("TC-MOD-007 [P] Xu ly bao cao khong hanh dong – chi doi trang thai")
        void handleReport_noAction_resolvesOnly() {
            Report report = new Report();
            report.setId(1L);
            report.setReportedUserId("user-005");
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));
            given(reportRepository.save(any(Report.class))).willAnswer(inv -> inv.getArgument(0));

            moderationService.handleReport(1L, GM_ID, "No issue found", "NONE");

            then(reportRepository).should().save(any(Report.class));
            then(redisTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-MOD-008 [P] Xu ly bao cao voi hanh dong MUTE – mute nguoi dung")
        void handleReport_muteAction_mutesUser() {
            Report report = new Report();
            report.setId(2L);
            report.setReportedUserId("user-010");
            given(reportRepository.findById(2L)).willReturn(Optional.of(report));
            given(reportRepository.save(any(Report.class))).willAnswer(inv -> inv.getArgument(0));
            given(redisTemplate.opsForValue()).willReturn(valueOps);

            moderationService.handleReport(2L, GM_ID, "Muting for spam", "MUTE");

            then(valueOps).should().set(contains("muted:user-010"), anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("TC-MOD-009 [N] Bao cao khong tim thay – nem RuntimeException")
        void handleReport_notFound_throwsException() {
            given(reportRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> moderationService.handleReport(999L, GM_ID, "Resolution", "NONE"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Report not found");
        }
    }

    // =========================================================
    // recordViolation
    // =========================================================
    @Nested
    @DisplayName("recordViolation()")
    class RecordViolation {

        @Test
        @DisplayName("TC-MOD-010 [P] Vi pham dau tien – chi canh bao (WARNING)")
        void recordViolation_firstOffense_warning() {
            given(violationRepository.countByUserIdAndCreatedAtAfter(eq("user-001"), any()))
                    .willReturn(0L); // < 2 → WARNING
            given(violationRepository.save(any(Violation.class))).willAnswer(inv -> inv.getArgument(0));

            moderationService.recordViolation("user-001", "BAD_WORD", "bad message", 1);

            then(violationRepository).should().save(argThat(v ->
                    "WARNING".equals(v.getActionTaken())));
            then(redisTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-MOD-011 [N] Vi pham >= maxViolations – tu dong ban")
        void recordViolation_maxViolationsReached_autoBan() {
            given(violationRepository.countByUserIdAndCreatedAtAfter(eq("user-001"), any()))
                    .willReturn(3L); // >= maxViolations(3) → BAN
            given(violationRepository.save(any(Violation.class))).willAnswer(inv -> inv.getArgument(0));
            given(redisTemplate.opsForValue()).willReturn(valueOps);

            moderationService.recordViolation("user-001", "BAD_WORD", "repeated", 1);

            then(violationRepository).should().save(argThat(v ->
                    "BAN".equals(v.getActionTaken())));
        }
    }

    // =========================================================
    // isUserMuted / isUserBanned
    // =========================================================
    @Nested
    @DisplayName("isUserMuted() / isUserBanned()")
    class MutedBanned {

        @Test
        @DisplayName("TC-MOD-012 [P] Nguoi dung bi mute – tra ve true")
        void isUserMuted_mutedUser_returnsTrue() {
            given(redisTemplate.hasKey("muted:user-007")).willReturn(true);

            assertThat(moderationService.isUserMuted("user-007")).isTrue();
        }

        @Test
        @DisplayName("TC-MOD-013 [P] Nguoi dung khong bi mute – tra ve false")
        void isUserMuted_notMuted_returnsFalse() {
            given(redisTemplate.hasKey("muted:user-007")).willReturn(false);

            assertThat(moderationService.isUserMuted("user-007")).isFalse();
        }

        @Test
        @DisplayName("TC-MOD-014 [P] Nguoi dung bi ban – tra ve true")
        void isUserBanned_bannedUser_returnsTrue() {
            given(redisTemplate.hasKey("banned:user-008")).willReturn(true);

            assertThat(moderationService.isUserBanned("user-008")).isTrue();
        }
    }

    // =========================================================
    // getUserViolations / getPendingReports
    // =========================================================
    @Nested
    @DisplayName("getUserViolations() / getPendingReports()")
    class Queries {

        @Test
        @DisplayName("TC-MOD-015 [P] Lay vi pham cua nguoi dung")
        void getUserViolations_returnsList() {
            Violation v = new Violation();
            v.setUserId("user-001");
            given(violationRepository.findByUserIdOrderByCreatedAtDesc("user-001")).willReturn(List.of(v));

            List<Violation> result = moderationService.getUserViolations("user-001");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-MOD-016 [P] Lay danh sach bao cao cho xu ly")
        void getPendingReports_returnsList() {
            Report r = new Report();
            r.setStatus("PENDING");
            given(reportRepository.findByStatusOrderByCreatedAtDesc("PENDING")).willReturn(List.of(r));

            List<Report> result = moderationService.getPendingReports();

            assertThat(result).hasSize(1);
        }
    }

    private static final String GM_ID = "gm-001";
}
