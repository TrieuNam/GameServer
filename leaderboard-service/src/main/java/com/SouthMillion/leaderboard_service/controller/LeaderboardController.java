package com.SouthMillion.leaderboard_service.controller;

import com.SouthMillion.leaderboard_service.dto.LeaderboardDTO;
import com.SouthMillion.leaderboard_service.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * Leaderboard REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Validated
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @PostMapping("/update")
    public ResponseEntity<LeaderboardDTO.Response<LeaderboardDTO.RankingInfo>> updateScore(
            @Valid @RequestBody LeaderboardDTO.UpdateScoreRequest request) {
        log.info("REST API: Update score - type={}, roleId={}", request.getRankingType(), request.getRoleId());
        return ResponseEntity.ok(leaderboardService.updateScore(request));
    }

    @GetMapping("/{rankingType}")
    public ResponseEntity<LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse>> getLeaderboard(
            @PathVariable Integer rankingType,
            @RequestParam(required = false) String roleId) {
        log.info("REST API: Get leaderboard - type={}, roleId={}", rankingType, roleId);
        return ResponseEntity.ok(leaderboardService.getLeaderboard(rankingType, roleId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshLeaderboards() {
        log.info("REST API: Manual refresh leaderboards");
        leaderboardService.refreshAllLeaderboards();
        return ResponseEntity.ok("All leaderboards refreshed!");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Leaderboard Service is running!");
    }
}
