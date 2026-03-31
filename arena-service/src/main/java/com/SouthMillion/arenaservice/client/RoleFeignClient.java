package com.SouthMillion.arenaservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for role-service — fetch role info for arena.
 *
 * Endpoints:
 *   GET /api/role/{roleId}/combat-power  → { fightPower, hp, atk, def, spd }
 *   GET /api/role/{roleId}/basic-info    → { name, level, fightPower }
 */
@FeignClient(name = "role-service")
public interface RoleFeignClient {

    @GetMapping("/api/role/{roleId}/combat-power")
    Map<String, Object> getCombatPower(@PathVariable("roleId") String roleId);

    /** Lightweight info: name + level + combat power (used by arena for display). */
    @GetMapping("/api/role/{roleId}/basic-info")
    Map<String, Object> getBasicInfo(@PathVariable("roleId") String roleId);
}

