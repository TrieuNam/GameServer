package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for pagoda-service (塔 — Tower/Pagoda system).
 * Two towers: ShiLian (试炼之塔) and GuMo (锢魔之塔).
 */
@FeignClient(name = "pagoda-service")
public interface PagodaFeign {

    // === ShiLian (Trial Tower 试炼之塔) ===

    @GetMapping("/api/pagoda/{roleId}/shilian")
    Map<String, Object> getShiLian(@PathVariable("roleId") String roleId);

    @PostMapping("/api/pagoda/{roleId}/shilian/challenge")
    Map<String, Object> challengeShiLian(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "p1", defaultValue = "0") int p1);

    @PostMapping("/api/pagoda/{roleId}/shilian/claim")
    Boolean claimShiLian(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "p1", defaultValue = "0") int p1);

    // === GuMo (Demon-Lock Tower 锢魔之塔) ===

    @GetMapping("/api/pagoda/{roleId}/gumo")
    Map<String, Object> getGuMo(@PathVariable("roleId") String roleId);

    @PostMapping("/api/pagoda/{roleId}/gumo/challenge")
    Map<String, Object> challengeGuMo(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "p1", defaultValue = "0") int p1);

    @PostMapping("/api/pagoda/{roleId}/gumo/claim")
    Boolean claimGuMo(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "p1", defaultValue = "0") int p1);
}
