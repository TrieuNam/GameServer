package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.LimitCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for LimitCore (CoreCrisis / 限界突破).
 *
 * <pre>
 * GET  /api/role/{roleId}/limit-core          → { coreLevels: [6 ints] }
 * POST /api/role/{roleId}/limit-core          → { coreLevels, [drawnItems] }
 *       body: { "type": 0|1, "p1": ... }
 *             type=0 LEVEL_UP  p1=limitType (1-6)
 *             type=1 DRAW      p1=boxType (0=free, 1=price1💎, 2=price2💎)
 * </pre>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/role/{roleId}/limit-core")
public class LimitCoreController {

    private final LimitCoreService svc;

    /** Fetch all 6 core levels — called at login bootstrap to push 1468. */
    @GetMapping
    public Map<String, Object> getInfo(@PathVariable Long roleId) {
        return Map.of("coreLevels", svc.getAllLevels(roleId));
    }

    /** Dispatch LEVEL_UP or DRAW. */
    @PostMapping
    public Map<String, Object> handle(
            @PathVariable Long roleId,
            @RequestBody(required = false) Map<String, Object> body) {
        int type = getInt(body, "type", 0);
        int p1   = getInt(body, "p1",   0);
        log.info("[LimitCore] POST roleId={} type={} p1={}", roleId, type, p1);
        return svc.handleRequest(roleId, type, p1);
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        if (map == null) return def;
        Object v = map.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }
}
