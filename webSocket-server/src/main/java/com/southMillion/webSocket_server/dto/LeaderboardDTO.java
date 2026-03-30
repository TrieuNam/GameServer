package com.SouthMillion.webSocket_server.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Leaderboard DTOs for Feign Client Integration
 */
public class LeaderboardDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateScoreRequest {
        private Integer rankingType; // 1-8
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long score;
        private String guildName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankingInfo {
        private Integer rank;
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long score;
        private Integer rankChange;
        private String guildName;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardResponse {
        private Integer rankingType;
        private String rankingName;
        private Integer totalPlayers;
        private List<RankingInfo> rankings;
        private RankingInfo myRank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response<T> {
        private Integer code;
        private String message;
        private T data;
    }
}
