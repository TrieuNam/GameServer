package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for gem-service (宝石图纸 — Gem Drawing system).
 */
@FeignClient(name = "gem-service")
public interface GemFeign {

    /** GET /api/gem/{roleId} — get all gem info */
    @GetMapping("/api/gem/{roleId}")
    Map<String, Object> getInfo(@PathVariable("roleId") String roleId);

    /** POST /api/gem/{roleId}/inlay — inlay gem into slot */
    @PostMapping("/api/gem/{roleId}/inlay")
    Map<String, Object> inlay(
            @PathVariable("roleId") String roleId,
            @RequestParam("gemId") Integer gemId,
            @RequestParam(value = "slotType", required = false) Integer slotType);

    /** POST /api/gem/{roleId}/remove — remove gem from slot */
    @PostMapping("/api/gem/{roleId}/remove")
    Map<String, Object> remove(
            @PathVariable("roleId") String roleId,
            @RequestParam("gemId") Integer gemId);

    /** POST /api/gem/{roleId}/compose — compose gems from source list */
    @PostMapping("/api/gem/{roleId}/compose")
    Map<String, Object> compose(
            @PathVariable("roleId") String roleId,
            @RequestBody List<Integer> sourceIds);

    /** POST /api/gem/{roleId}/upgrade-all — upgrade gems in batch */
    @PostMapping("/api/gem/{roleId}/upgrade-all")
    Map<String, Object> upgradeAll(
            @PathVariable("roleId") String roleId,
            @RequestBody List<Integer> itemIds);

    /** POST /api/gem/{roleId}/buy — buy gem drawings */
    @PostMapping("/api/gem/{roleId}/buy")
    Map<String, Object> buy(
            @PathVariable("roleId") String roleId,
            @RequestBody List<Integer> itemIds);
}
