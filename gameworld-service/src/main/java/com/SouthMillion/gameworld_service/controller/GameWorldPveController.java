package com.SouthMillion.gameworld_service.controller;

import com.SouthMillion.gameworld_service.dto.PlayerPosition;
import com.SouthMillion.gameworld_service.service.GameWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
@RequestMapping("/api/gameworld")
@RequiredArgsConstructor
public class GameWorldPveController {

    private final GameWorldService gameWorldService;

    @Value("${gameworld.battle.report-dir:${java.io.tmpdir}/battlereports}")
    private String battleReportDir;

    /**
     * Lightweight adventure instance creation endpoint expected by `main-fb-service`.
     * Also writes a minimal battle data file so the client can fetch it.
     */
    @PostMapping("/pve/instance")
    public ResponseEntity<Map<String, Object>> createPveInstance(@RequestBody Map<String, Object> req) {
        String playerId = String.valueOf(req.getOrDefault("playerId", "0"));
        int stage = parseInt(req.get("stage"), 0);
        int level = parseInt(req.get("level"), 0);

        Long roleId = parseLong(req.get("playerId"));
        if (roleId != null && roleId > 0) {
            PlayerPosition position = PlayerPosition.builder()
                    .playerId(roleId)
                    .mapId(Math.max(1, stage * 1000 + level))
                    .x(0f)
                    .y(0f)
                    .z(0f)
                    .state("PVE")
                    .build();
            gameWorldService.updatePosition(position);
        }

        long now = System.currentTimeMillis();
        String battleId = "mainfb-%s-%d-%d-%d".formatted(playerId, stage, level, now);
        long seed = Math.abs(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
        long expireAt = now + (5 * 60 * 1000L);

        writeBattleReport(battleId, playerId);

        log.info("[GameWorldPveController] Created PVE instance playerId={} stage={}-{} battleId={}", playerId, stage, level, battleId);
        return ResponseEntity.ok(Map.of(
                "battleId", battleId,
                "seed", seed,
                "expireAt", expireAt
        ));
    }

    /**
     * Writes a minimal valid battle data file so the client can fetch and render it.
     *
     * Event format: one event per line — "type p1 [p2] [p3] ..."
     *
     *   type 2 = CHARACTER_INFO  p1=position(0-5)  p2=charType(0=ROLE,1=MONSTER,2=PET)  p3=id
     *   type 1 = ROUND_BEGIN     p1=round_number
     *   type 8 = ROUND_END
     *   type 9 = FIGHT_OVER      p1=result(1=WIN, 2=LOSE)
     *
     * CHARACTER_INFO is required: the client counts characters to know when all spines have
     * loaded. Without at least one CHARACTER_INFO the spine-load callback never fires and
     * the battle hangs in LOAD state forever.
     *
     * Positions: 0-2 = hero side (left), 3-5 = enemy side (right).
     * ROLE type (p2=0) uses the default hero spine — no config lookup on the client, safe to use
     * with any playerId value.
     */
    private void writeBattleReport(String battleId, String playerId) {
        try {
            Path dir = Paths.get(battleReportDir);
            Files.createDirectories(dir);
            Path file = dir.resolve(battleId);
            if (!Files.exists(file)) {
                // CHARACTER_INFO for the player at position 0 (ROLE type, id=playerId)
                String battleData = "2 0 0 " + playerId + "\n"
                        + "1 1\n"   // ROUND_BEGIN round 1
                        + "8\n"     // ROUND_END
                        + "9 1\n";  // FIGHT_OVER WIN
                Files.writeString(file, battleData, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.debug("[GameWorldPveController] Wrote battle report battleId={} playerId={} to {}",
                        battleId, playerId, file);
            }
        } catch (Exception e) {
            log.warn("[GameWorldPveController] Failed to write battle report battleId={}: {}", battleId, e.getMessage());
        }
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return defaultValue; }
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return null; }
    }
}
