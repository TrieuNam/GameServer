package com.SouthMillion.task_service.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for leaderboard-service integration.
 * Used by ArenaEventConsumer to update arena rankings after each match.
 */
@FeignClient(name = "leaderboard-service", path = "/api/leaderboard")
public interface LeaderboardClient {

    /**
     * Update a player's score on the leaderboard.
     * POST /api/leaderboard/update
     */
    @PostMapping("/update")
    Map<String, Object> updateScore(@RequestBody UpdateScoreRequest request);

    // ---- Local DTO (mirrors leaderboard-service LeaderboardDTO.UpdateScoreRequest) ----

    /** Arena ranking type constant (matches leaderboard-service config). */
    int RANKING_TYPE_ARENA = 2;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class UpdateScoreRequest {
        private Integer rankingType;
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long score;
        private String guildName;
    }
}
