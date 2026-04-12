package com.SouthMillion.rune_service.controller;

import com.SouthMillion.rune_service.repository.RuneTowerStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Internal endpoints called by scheduler-service or other backend services.
 * Not exposed to clients.
 */
@RestController
@RequestMapping("/internal/rune")
@RequiredArgsConstructor
@Slf4j
public class InternalRuneController {

    private final RuneTowerStateRepository towerStateRepository;

    /**
     * Reset daily_reward = 0 for all players.
     * Called by scheduler-service DailyResetJob at midnight every day.
     */
    @PostMapping("/reset/daily")
    @Transactional
    public Map<String, Object> resetDailyReward() {
        log.info("[InternalRune] Starting daily reward reset at {}", LocalDateTime.now());
        try {
            towerStateRepository.resetAllDailyRewards();
            log.info("[InternalRune] Daily reward reset completed");
            return Map.of("success", true, "message", "Daily rune reward reset completed");
        } catch (Exception e) {
            log.error("[InternalRune] Daily reward reset failed: {}", e.getMessage(), e);
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
