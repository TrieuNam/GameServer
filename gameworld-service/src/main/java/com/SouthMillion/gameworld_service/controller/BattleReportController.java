package com.SouthMillion.gameworld_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Serves battle report files at the path the client expects:
 *   /{serverPrefix}/fightdata/common_{pve|pvp}/{battleModeType}/{battleId}
 *
 * Example: /h02_dev_s11/fightdata/common_pve/0/mainfb-12-1-1-1234567890
 *
 * In local dev this service also listens on port 85 (see LocalBattlePortConfig),
 * so the client can fetch files directly without a separate nginx proxy.
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class BattleReportController {

    @Value("${gameworld.battle.report-dir:${java.io.tmpdir}/battlereports}")
    private String battleReportDir;

    /**
     * Supports both `/fightdata/...` and `/{serverPrefix}/fightdata/...` URLs
     * while staying compatible with Spring Boot 3's PathPattern parser.
     */
    @GetMapping({"/fightdata/{*battlePath}", "/{serverPrefix}/fightdata/{*battlePath}"})
    public ResponseEntity<String> serveBattleReport(
            @PathVariable String battlePath,
            HttpServletRequest request) {
        String battleId = Arrays.stream(battlePath.split("/"))
                .filter(part -> part != null && !part.isBlank())
                .reduce((first, second) -> second)
                .orElse("");

        if (battleId.isBlank()) {
            log.warn("[BattleReport] Missing battleId for uri={}", request.getRequestURI());
            return ResponseEntity.badRequest().build();
        }

        try {
            Path baseDir = Paths.get(battleReportDir).normalize();
            Path file = baseDir.resolve(battleId).normalize();
            if (!file.startsWith(baseDir)) {
                log.warn("[BattleReport] Path traversal attempt battleId={}", battleId);
                return ResponseEntity.badRequest().build();
            }
            if (!Files.exists(file)) {
                log.warn("[BattleReport] File not found battleId={} path={}", battleId, file);
                return ResponseEntity.notFound().build();
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            log.debug("[BattleReport] Served battleId={}", battleId);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(content);
        } catch (Exception e) {
            log.error("[BattleReport] Error serving battleId={}: {}", battleId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
