package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for knights-service (骑士手册 — Knights Handbook system).
 */
@FeignClient(name = "knights-service")
public interface KnightsFeign {

    /** GET /api/knights/{roleId} — get or create handbook info */
    @GetMapping("/api/knights/{roleId}")
    Map<String, Object> getInfo(@PathVariable("roleId") String roleId);

    /** GET /api/knights/{roleId}/conditions — get condition list */
    @GetMapping("/api/knights/{roleId}/conditions")
    Map<String, Object> getConditions(@PathVariable("roleId") String roleId);

    /** POST /api/knights/{roleId}/claim-seq — claim sequential reward */
    @PostMapping("/api/knights/{roleId}/claim-seq")
    Map<String, Object> claimSeq(
            @PathVariable("roleId") String roleId,
            @RequestParam("seqIndex") int seqIndex);

    /** POST /api/knights/{roleId}/claim-level — claim level reward */
    @PostMapping("/api/knights/{roleId}/claim-level")
    Map<String, Object> claimLevel(
            @PathVariable("roleId") String roleId,
            @RequestParam("levelIndex") int levelIndex);
}
