package com.SouthMillion.analytics_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.analytics_service.entity.PlayerEvent;
import com.SouthMillion.analytics_service.entity.PlayerKpi;
import com.SouthMillion.analytics_service.repository.PlayerEventRepository;
import com.SouthMillion.analytics_service.repository.PlayerKpiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock private PlayerEventRepository eventRepository;
    @Mock private PlayerKpiRepository   kpiRepository;
    @Mock private KpiUpdateService      kpiUpdateService;
    @Spy  private ObjectMapper          objectMapper = new ObjectMapper();

    @InjectMocks private AnalyticsService analyticsService;

    // =========================================================
    // trackEvent
    // =========================================================
    @Nested
    @DisplayName("trackEvent()")
    class TrackEvent {

        @Test
        @DisplayName("TC-ANA-001 [P] Ghi nhan su kien login – luu event va cap nhat KPI")
        void trackEvent_login_savesEventAndUpdatesKpi() {
            PlayerEvent saved = new PlayerEvent();
            saved.setId(1L);
            saved.setPlayerId(10L);
            saved.setEventType("player.login");
            given(eventRepository.save(any(PlayerEvent.class))).willReturn(saved);
            willDoNothing().given(kpiUpdateService).updateKpiMetrics(anyLong(), anyString(), any());

            PlayerEvent result = analyticsService.trackEvent(10L, "player.login", "gameplay",
                    Map.of(), "session-1");

            assertThat(result.getId()).isEqualTo(1L);
            then(eventRepository).should().save(any(PlayerEvent.class));
            then(kpiUpdateService).should().updateKpiMetrics(eq(10L), eq("player.login"), any());
        }

        @Test
        @DisplayName("TC-ANA-002 [P] Ghi nhan su kien moi khi chua co KPI hom nay")
        void trackEvent_noExistingKpi_createsNewKpi() {
            PlayerEvent saved = new PlayerEvent();
            saved.setId(2L);
            given(eventRepository.save(any(PlayerEvent.class))).willReturn(saved);
            willDoNothing().given(kpiUpdateService).updateKpiMetrics(anyLong(), anyString(), any());

            PlayerEvent result = analyticsService.trackEvent(10L, "player.login", "gameplay",
                    Map.of(), "session-2");

            assertThat(result).isNotNull();
            then(kpiUpdateService).should().updateKpiMetrics(eq(10L), eq("player.login"), any());
        }
    }

    @Nested
    @DisplayName("normalizeSessionId()")
    class NormalizeSessionId {

        @Test
        @DisplayName("TC-ANA-014 [P] sessionId rong hoac blank – luu null")
        void normalizeSessionId_blank_returnsNull() {
            assertThat(analyticsService.normalizeSessionId("   ")).isNull();
            assertThat(analyticsService.normalizeSessionId(null)).isNull();
        }

        @Test
        @DisplayName("TC-ANA-015 [P] sessionId la JWT dai – trich xuat sid de luu")
        void normalizeSessionId_oversizedJwt_extractsSid() {
            String sid = "fd7cc080cb65451fa162ba3fb84d81b3";
            String jwt = encodeBase64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}")
                    + "."
                    + encodeBase64Url("{\"sub\":\"user-1\",\"sid\":\"" + sid + "\",\"typ\":\"access\"}")
                    + ".signature-placeholder";

            assertThat(jwt.length()).isGreaterThan(AnalyticsService.SESSION_ID_MAX_LENGTH);
            assertThat(analyticsService.normalizeSessionId(jwt)).isEqualTo(sid);
        }

        @Test
        @DisplayName("TC-ANA-016 [P] sessionId dai khong hop le – cat ngan an toan theo gioi han cot")
        void normalizeSessionId_oversizedNonJwt_truncatesToColumnLimit() {
            String oversized = "x".repeat(AnalyticsService.SESSION_ID_MAX_LENGTH + 30);

            String normalized = analyticsService.normalizeSessionId(oversized);

            assertThat(normalized)
                    .hasSize(AnalyticsService.SESSION_ID_MAX_LENGTH)
                    .isEqualTo(oversized.substring(0, AnalyticsService.SESSION_ID_MAX_LENGTH));
        }

        private String encodeBase64Url(String value) {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    // =========================================================
    // KpiUpdateService – individual event types
    // (these tests now target KpiUpdateService directly)
    // =========================================================
    @Nested
    @DisplayName("updateKpiMetrics() – event types")
    class UpdateKpiMetrics {

        private KpiUpdateService kpiSvc() {
            return new KpiUpdateService(kpiRepository);
        }

        @Test
        @DisplayName("TC-ANA-003 [P] Logout voi duration – tang sessionDuration")
        void updateKpiMetrics_logout_incrementsSessionDuration() {
            PlayerKpi kpi = new PlayerKpi();
            kpi.setSessionDuration(100);
            given(kpiRepository.findByPlayerIdAndDate(eq(1L), any())).willReturn(Optional.of(kpi));
            given(kpiRepository.save(any(PlayerKpi.class))).willAnswer(inv -> inv.getArgument(0));

            kpiSvc().updateKpiMetrics(1L, "player.logout", Map.of("duration", 300));

            then(kpiRepository).should().save(argThat(k -> k.getSessionDuration() == 400));
        }

        @Test
        @DisplayName("TC-ANA-004 [P] shop.purchase voi amount – tang totalSpent va purchaseCount")
        void updateKpiMetrics_shopPurchase_incrementsSpentAndCount() {
            PlayerKpi kpi = new PlayerKpi();
            kpi.setTotalSpent(BigDecimal.ZERO);
            kpi.setPurchaseCount(0);
            given(kpiRepository.findByPlayerIdAndDate(eq(1L), any())).willReturn(Optional.of(kpi));
            given(kpiRepository.save(any(PlayerKpi.class))).willAnswer(inv -> inv.getArgument(0));

            kpiSvc().updateKpiMetrics(1L, "shop.purchase", Map.of("amount", 500));

            then(kpiRepository).should().save(argThat(k ->
                    k.getPurchaseCount() == 1 &&
                    k.getTotalSpent().compareTo(new BigDecimal("500")) == 0));
        }

        @Test
        @DisplayName("TC-ANA-005 [P] battle.won – tang battlesWon")
        void updateKpiMetrics_battleWon_incrementsBattlesWon() {
            PlayerKpi kpi = new PlayerKpi();
            kpi.setBattlesWon(5);
            given(kpiRepository.findByPlayerIdAndDate(eq(1L), any())).willReturn(Optional.of(kpi));
            given(kpiRepository.save(any(PlayerKpi.class))).willAnswer(inv -> inv.getArgument(0));

            kpiSvc().updateKpiMetrics(1L, "battle.won", Map.of());

            then(kpiRepository).should().save(argThat(k -> k.getBattlesWon() == 6));
        }

        @Test
        @DisplayName("TC-ANA-006 [P] task.completed – tang tasksCompleted")
        void updateKpiMetrics_taskCompleted_incrementsTasks() {
            PlayerKpi kpi = new PlayerKpi();
            kpi.setTasksCompleted(10);
            given(kpiRepository.findByPlayerIdAndDate(eq(1L), any())).willReturn(Optional.of(kpi));
            given(kpiRepository.save(any(PlayerKpi.class))).willAnswer(inv -> inv.getArgument(0));

            kpiSvc().updateKpiMetrics(1L, "task.completed", Map.of());

            then(kpiRepository).should().save(argThat(k -> k.getTasksCompleted() == 11));
        }

        @Test
        @DisplayName("TC-ANA-007 [P] Su kien khong ro – khong thay doi KPI")
        void updateKpiMetrics_unknownEvent_noChange() {
            PlayerKpi kpi = new PlayerKpi();
            kpi.setLoginCount(3);
            given(kpiRepository.findByPlayerIdAndDate(eq(1L), any())).willReturn(Optional.of(kpi));
            given(kpiRepository.save(any(PlayerKpi.class))).willAnswer(inv -> inv.getArgument(0));

            kpiSvc().updateKpiMetrics(1L, "unknown.event", Map.of());

            then(kpiRepository).should().save(argThat(k -> k.getLoginCount() == 3));
        }
    }

    // =========================================================
    // getPlayerEvents
    // =========================================================
    @Nested
    @DisplayName("getPlayerEvents()")
    class GetPlayerEvents {

        @Test
        @DisplayName("TC-ANA-008 [P] Lay su kien nguoi choi theo khoang thoi gian")
        void getPlayerEvents_returnsEvents() {
            PlayerEvent e1 = new PlayerEvent();
            e1.setId(1L);
            e1.setEventType("player.login");
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end   = LocalDateTime.now();
            given(eventRepository.findByPlayerIdAndEventTimeBetween(eq(1L), eq(start), eq(end)))
                    .willReturn(List.of(e1));

            List<PlayerEvent> result = analyticsService.getPlayerEvents(1L, start, end);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEventType()).isEqualTo("player.login");
        }

        @Test
        @DisplayName("TC-ANA-009 [P] Lay su kien theo loai – tra ve danh sach")
        void getPlayerEventsByType_returnsFiltered() {
            PlayerEvent e = new PlayerEvent();
            e.setEventType("battle.started");
            given(eventRepository.findByPlayerIdAndEventType(eq(1L), eq("battle.started")))
                    .willReturn(List.of(e));

            List<PlayerEvent> result = analyticsService.getPlayerEventsByType(1L, "battle.started");

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================
    // getPlayerKpi
    // =========================================================
    @Nested
    @DisplayName("getPlayerKpi()")
    class GetPlayerKpi {

        @Test
        @DisplayName("TC-ANA-010 [P] Lay KPI hom nay – tra ve KPI")
        void getPlayerKpi_found_returnsKpi() {
            PlayerKpi kpi = new PlayerKpi();
            kpi.setPlayerId(1L);
            kpi.setLoginCount(5);
            LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            given(kpiRepository.findByPlayerIdAndDate(eq(1L), eq(today)))
                    .willReturn(Optional.of(kpi));

            PlayerKpi result = analyticsService.getPlayerKpi(1L, today);

            assertThat(result).isNotNull();
            assertThat(result.getLoginCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-ANA-011 [P] KPI chua co – tra ve null")
        void getPlayerKpi_notFound_returnsNull() {
            given(kpiRepository.findByPlayerIdAndDate(eq(1L), any())).willReturn(Optional.empty());

            PlayerKpi result = analyticsService.getPlayerKpi(1L, LocalDateTime.now());

            assertThat(result).isNull();
        }
    }

    // =========================================================
    // getTopSpenders / getMostActiveUsers
    // =========================================================
    @Nested
    @DisplayName("getTopSpenders() / getMostActiveUsers()")
    class TopMetrics {

        @Test
        @DisplayName("TC-ANA-012 [P] Lay top nguoi chi nhieu nhat")
        void getTopSpenders_returnsList() {
            PlayerKpi kpi = new PlayerKpi();
            kpi.setTotalSpent(new BigDecimal("9999"));
            given(kpiRepository.findTopSpenders(any(LocalDateTime.class))).willReturn(List.of(kpi));

            List<PlayerKpi> result = analyticsService.getTopSpenders(LocalDateTime.now().minusDays(7));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-ANA-013 [P] Lay nguoi choi hoat dong nhat")
        void getMostActiveUsers_returnsList() {
            given(kpiRepository.findMostActiveUsers(any(LocalDateTime.class))).willReturn(List.of());

            List<PlayerKpi> result = analyticsService.getMostActiveUsers(LocalDateTime.now().minusDays(30));

            assertThat(result).isEmpty();
        }
    }
}
