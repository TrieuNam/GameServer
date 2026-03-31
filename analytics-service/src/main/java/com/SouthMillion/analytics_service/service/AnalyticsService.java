package com.SouthMillion.analytics_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.SouthMillion.analytics_service.entity.PlayerEvent;
import com.SouthMillion.analytics_service.entity.PlayerKpi;
import com.SouthMillion.analytics_service.repository.PlayerEventRepository;
import com.SouthMillion.analytics_service.repository.PlayerKpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    static final int SESSION_ID_MAX_LENGTH = 50;

    private final PlayerEventRepository eventRepository;
    private final PlayerKpiRepository kpiRepository;
    private final KpiUpdateService kpiUpdateService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PlayerEvent trackEvent(Long playerId, String eventType, String eventCategory,
                                   Map<String, Object> eventData, String sessionId) {
        try {
            PlayerEvent event = new PlayerEvent();
            event.setPlayerId(playerId);
            event.setEventType(eventType);
            event.setEventCategory(eventCategory);
            event.setEventData(objectMapper.writeValueAsString(eventData));
            event.setEventTime(LocalDateTime.now());
            event.setSessionId(normalizeSessionId(sessionId));
            event.setServerName("server-1");

            PlayerEvent saved = eventRepository.save(event);
            log.info("Tracked event: {} for player: {}", eventType, playerId);

            // Update KPI metrics in its own independent transaction.
            // Retry once on duplicate-key (race condition: two concurrent requests inserting
            // the same player+date row).
            try {
                kpiUpdateService.updateKpiMetrics(playerId, eventType, eventData);
            } catch (DataIntegrityViolationException e) {
                log.warn("Duplicate KPI row for player {} on {}; retrying update.",
                        playerId, LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
                kpiUpdateService.updateKpiMetrics(playerId, eventType, eventData);
            }

            return saved;
        } catch (Exception e) {
            log.error("Failed to track event: {} for player: {}", eventType, playerId, e);
            throw new RuntimeException("Failed to track event", e);
        }
    }

    String normalizeSessionId(String rawSessionId) {
        if (rawSessionId == null) {
            return null;
        }

        String sessionId = rawSessionId.trim();
        if (sessionId.isEmpty()) {
            return null;
        }
        if (sessionId.length() <= SESSION_ID_MAX_LENGTH) {
            return sessionId;
        }

        String sidFromJwt = extractSidFromJwt(sessionId);
        if (sidFromJwt != null && sidFromJwt.length() <= SESSION_ID_MAX_LENGTH) {
            log.debug("Normalized analytics sessionId from JWT len={} to sid len={}",
                    sessionId.length(), sidFromJwt.length());
            return sidFromJwt;
        }

        log.warn("Oversized analytics sessionId len={} could not be normalized; truncating for storage",
                sessionId.length());
        return sessionId.substring(0, SESSION_ID_MAX_LENGTH);
    }

    private String extractSidFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || parts[1].isBlank()) {
                return null;
            }

            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode sidNode = claims.get("sid");
            if (sidNode == null || sidNode.isNull()) {
                return null;
            }

            String sid = sidNode.asText(null);
            return sid == null || sid.isBlank() ? null : sid.trim();
        } catch (Exception e) {
            log.debug("Failed to extract sid from oversized analytics sessionId: {}", e.getMessage());
            return null;
        }
    }

    public List<PlayerEvent> getPlayerEvents(Long playerId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByPlayerIdAndEventTimeBetween(playerId, start, end);
    }

    public List<PlayerEvent> getPlayerEventsByType(Long playerId, String eventType) {
        return eventRepository.findByPlayerIdAndEventType(playerId, eventType);
    }

    public PlayerKpi getPlayerKpi(Long playerId, LocalDateTime date) {
        return kpiRepository.findByPlayerIdAndDate(playerId, date).orElse(null);
    }

    public List<PlayerKpi> getPlayerKpiRange(Long playerId, LocalDateTime start, LocalDateTime end) {
        return kpiRepository.findByPlayerIdAndDateBetween(playerId, start, end);
    }

    public List<PlayerKpi> getTopSpenders(LocalDateTime since) {
        return kpiRepository.findTopSpenders(since);
    }

    public List<PlayerKpi> getMostActiveUsers(LocalDateTime since) {
        return kpiRepository.findMostActiveUsers(since);
    }
}
