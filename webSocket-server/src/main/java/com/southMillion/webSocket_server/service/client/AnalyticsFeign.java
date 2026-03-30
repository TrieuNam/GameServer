package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "analytics-service")
public interface AnalyticsFeign {
    
    @PostMapping("/api/analytics/track")
    Map<String, Object> trackEvent(@RequestBody Map<String, Object> request);
    
    @GetMapping("/api/analytics/events/{playerId}")
    List<Map<String, Object>> getPlayerEvents(
        @PathVariable("playerId") Long playerId,
        @RequestParam String start,
        @RequestParam String end
    );
    
    @GetMapping("/api/analytics/kpi/{playerId}")
    Map<String, Object> getPlayerKpi(@PathVariable("playerId") Long playerId, @RequestParam String date);
}
