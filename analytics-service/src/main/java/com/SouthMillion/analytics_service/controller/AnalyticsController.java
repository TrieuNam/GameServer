package com.SouthMillion.analytics_service.controller;

import com.SouthMillion.analytics_service.entity.PlayerEvent;
import com.SouthMillion.analytics_service.entity.PlayerKpi;
import com.SouthMillion.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> trackEvent(@RequestBody Map<String, Object> request) {
        Object playerIdRaw = request.get("playerId");
        if (playerIdRaw == null) {
            // playerId is required — skip silently instead of NPE → 500
            return ResponseEntity.ok(Map.of("success", false, "message", "playerId missing — event skipped"));
        }
        Long playerId = ((Number) playerIdRaw).longValue();
        String eventType = (String) request.get("eventType");
        String eventCategory = (String) request.get("eventCategory");
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) request.getOrDefault("eventData", Map.of());
        String sessionId = (String) request.get("sessionId");
        
        PlayerEvent event = analyticsService.trackEvent(playerId, eventType, eventCategory, eventData, sessionId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "eventId", event.getId(),
            "message", "Event tracked successfully"
        ));
    }
    
    @GetMapping("/events/{playerId}")
    public ResponseEntity<List<PlayerEvent>> getPlayerEvents(
        @PathVariable Long playerId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        List<PlayerEvent> events = analyticsService.getPlayerEvents(playerId, start, end);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events/{playerId}/type/{eventType}")
    public ResponseEntity<List<PlayerEvent>> getPlayerEventsByType(
        @PathVariable Long playerId,
        @PathVariable String eventType
    ) {
        List<PlayerEvent> events = analyticsService.getPlayerEventsByType(playerId, eventType);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/kpi/{playerId}")
    public ResponseEntity<PlayerKpi> getPlayerKpi(
        @PathVariable Long playerId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date
    ) {
        PlayerKpi kpi = analyticsService.getPlayerKpi(playerId, date);
        return kpi != null ? ResponseEntity.ok(kpi) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/kpi/{playerId}/range")
    public ResponseEntity<List<PlayerKpi>> getPlayerKpiRange(
        @PathVariable Long playerId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        List<PlayerKpi> kpis = analyticsService.getPlayerKpiRange(playerId, start, end);
        return ResponseEntity.ok(kpis);
    }
    
    @GetMapping("/top-spenders")
    public ResponseEntity<List<PlayerKpi>> getTopSpenders(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since
    ) {
        List<PlayerKpi> topSpenders = analyticsService.getTopSpenders(since);
        return ResponseEntity.ok(topSpenders);
    }
    
    @GetMapping("/most-active")
    public ResponseEntity<List<PlayerKpi>> getMostActiveUsers(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since
    ) {
        List<PlayerKpi> activeUsers = analyticsService.getMostActiveUsers(since);
        return ResponseEntity.ok(activeUsers);
    }
}
