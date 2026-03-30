package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for lingzhu-service (领主副本 — Lord Dungeon system).
 * NOTE: No hardcoded url — uses Eureka service discovery (port 8380).
 * The old default was localhost:8300 which conflicts with trial-service.
 */
@FeignClient(name = "lingzhu-service")
public interface LingZhuFeign {

    /** GET /api/lingzhu/{roleId} — list all dungeon progress for role */
    @GetMapping("/api/lingzhu/{roleId}")
    List<Map<String, Object>> getAll(@PathVariable("roleId") String roleId);

    /** POST /api/lingzhu/{roleId}/challenge — challenge a dungeon stage */
    @PostMapping("/api/lingzhu/{roleId}/challenge")
    Map<String, Object> challenge(
            @PathVariable("roleId") String roleId,
            @RequestParam("stage") int stage,
            @RequestParam(value = "p1", defaultValue = "0") int p1);

    /** POST /api/lingzhu/{roleId}/sweep — sweep (auto-clear) a dungeon stage */
    @PostMapping("/api/lingzhu/{roleId}/sweep")
    Map<String, Object> sweep(
            @PathVariable("roleId") String roleId,
            @RequestParam("stage") int stage,
            @RequestParam(value = "count", defaultValue = "1") int count);
}
