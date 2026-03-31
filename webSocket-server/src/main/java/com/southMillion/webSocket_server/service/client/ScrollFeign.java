package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for scroll-service (卷轴 — Scroll/Lottery system).
 */
@FeignClient(name = "scroll-service")
public interface ScrollFeign {

    /** GET /api/scroll/{roleId}/info — get scroll meta (freeNum, baoDiNum) */
    @GetMapping("/api/scroll/{roleId}/info")
    Map<String, Object> getInfo(@PathVariable("roleId") String roleId);

    /** GET /api/scroll/{roleId}/list — get all scroll items */
    @GetMapping("/api/scroll/{roleId}/list")
    List<Map<String, Object>> getList(@PathVariable("roleId") String roleId);

    /** POST /api/scroll/{roleId}/draw — draw scrolls */
    @PostMapping("/api/scroll/{roleId}/draw")
    Map<String, Object> draw(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "count", defaultValue = "1") int count);
}
