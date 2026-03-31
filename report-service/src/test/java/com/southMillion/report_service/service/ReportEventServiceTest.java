package com.SouthMillion.report_service.service;

import com.SouthMillion.report_service.dto.ReportStatsDTO;
import com.SouthMillion.report_service.entity.ReportEvent;
import com.SouthMillion.report_service.enums.ReportEventType;
import com.SouthMillion.report_service.repository.ReportEventRepository;
import org.SouthMillion.dto.report.ReportResultDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportEventService Tests")
class ReportEventServiceTest {

    @Mock private ReportEventRepository repository;

    @InjectMocks private ReportEventService reportEventService;

    // =========================================================
    // save
    // =========================================================
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("TC-REPORT-001 [P] Luu event thanh cong")
        void save_validEvent_returnsSaved() {
            ReportEvent event = new ReportEvent();
            event.setType(ReportEventType.APP_START.getCode());
            given(repository.save(any(ReportEvent.class))).willReturn(event);

            ReportEvent result = reportEventService.save(event);

            assertThat(result).isNotNull();
            then(repository).should().save(event);
        }
    }

    // =========================================================
    // processReportToDTO
    // =========================================================
    @Nested
    @DisplayName("processReportToDTO()")
    class ProcessReportToDTO {

        @Test
        @DisplayName("TC-REPORT-002 [P] Du lieu base64 hop le – giai ma thanh cong")
        void processReportToDTO_validBase64_returnsOk() {
            String payload = "Hello, Report!";
            String encoded = Base64.getEncoder().encodeToString(payload.getBytes());

            ReportResultDTO result = reportEventService.processReportToDTO(encoded);

            assertThat(result.getStatus()).isEqualTo("ok");
            assertThat(result.getDecodedData()).isEqualTo(payload);
        }

        @Test
        @DisplayName("TC-REPORT-003 [N] Du lieu base64 khong hop le – tra ve trang thai loi")
        void processReportToDTO_invalidBase64_returnsError() {
            ReportResultDTO result = reportEventService.processReportToDTO("!!!not-base64!!!");

            assertThat(result.getStatus()).isEqualTo("error");
            assertThat(result.getDecodedData()).isNull();
        }
    }

    // =========================================================
    // findByType
    // =========================================================
    @Nested
    @DisplayName("findByType()")
    class FindByType {

        @Test
        @DisplayName("TC-REPORT-004 [P] Tim bao cao theo loai")
        void findByType_returnsMatchingReports() {
            ReportEvent event = new ReportEvent();
            event.setType(ReportEventType.USER_LOGIN.getCode());
            given(repository.findByType(ReportEventType.USER_LOGIN.getCode())).willReturn(List.of(event));

            List<ReportEvent> result = reportEventService.findByType(ReportEventType.USER_LOGIN.getCode());

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================
    // findByDeviceId
    // =========================================================
    @Nested
    @DisplayName("findByDeviceId()")
    class FindByDeviceId {

        @Test
        @DisplayName("TC-REPORT-005 [P] Tim bao cao theo device ID")
        void findByDeviceId_returnsMatchingReports() {
            ReportEvent event = new ReportEvent();
            given(repository.findByDeviceId("device-ABC")).willReturn(List.of(event));

            List<ReportEvent> result = reportEventService.findByDeviceId("device-ABC");

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================
    // findByDateRange
    // =========================================================
    @Nested
    @DisplayName("findByDateRange()")
    class FindByDateRange {

        @Test
        @DisplayName("TC-REPORT-006 [P] Tim bao cao theo khoang thoi gian")
        void findByDateRange_returnsMatchingReports() {
            long start = Instant.now().minusSeconds(3600).toEpochMilli();
            long end   = Instant.now().toEpochMilli();
            ReportEvent event = new ReportEvent();
            given(repository.findByEventTimeBetween(start, end)).willReturn(List.of(event));

            List<ReportEvent> result = reportEventService.findByDateRange(start, end);

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================
    // getStatistics
    // =========================================================
    @Nested
    @DisplayName("getStatistics()")
    class GetStatistics {

        @Test
        @DisplayName("TC-REPORT-007 [P] Lay thong ke toan bo – tra ve ReportStatsDTO")
        void getStatistics_returnsStats() {
            given(repository.count()).willReturn(100L);
            given(repository.countByCreatedAtAfter(any(Instant.class))).willReturn(10L);
            given(repository.countDistinctDeviceIdByCreatedAtAfter(any(Instant.class))).willReturn(5L);
            given(repository.countDistinctSessionIdByCreatedAtAfter(any(Instant.class))).willReturn(8L);
            given(repository.countByType(ReportEventType.APP_START.getCode())).willReturn(20L);
            given(repository.countByType(ReportEventType.APP_EXIT.getCode())).willReturn(18L);
            given(repository.countByType(ReportEventType.USER_LOGIN.getCode())).willReturn(15L);
            given(repository.countByType(ReportEventType.USER_LOGOUT.getCode())).willReturn(12L);
            given(repository.countByType(ReportEventType.ERROR_REPORT.getCode())).willReturn(3L);
            given(repository.countByType(ReportEventType.CUSTOM.getCode())).willReturn(32L);

            ReportStatsDTO stats = reportEventService.getStatistics();

            assertThat(stats.getTotalReports()).isEqualTo(100L);
            assertThat(stats.getTodayReports()).isEqualTo(10L);
            assertThat(stats.getActiveDevices()).isEqualTo(5L);
        }

        @Test
        @DisplayName("TC-REPORT-008 [N] Loi khi lay thong ke – tra ve zero stats")
        void getStatistics_repositoryError_returnsZeroStats() {
            given(repository.count()).willThrow(new RuntimeException("DB error"));

            ReportStatsDTO stats = reportEventService.getStatistics();

            assertThat(stats.getTotalReports()).isEqualTo(0L);
            assertThat(stats.getTodayReports()).isEqualTo(0L);
        }
    }
}
