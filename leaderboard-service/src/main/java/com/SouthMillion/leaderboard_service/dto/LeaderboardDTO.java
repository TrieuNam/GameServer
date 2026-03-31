package com.SouthMillion.leaderboard_service.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class LeaderboardDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateScoreRequest {
        @NotNull
        @Min(1) @Max(8)
        private Integer rankingType;
        
        @NotBlank
        private String roleId;
        
        @NotBlank
        private String roleName;
        
        @NotNull
        @Min(1)
        private Integer roleLevel;
        
        @NotNull
        @Min(0)
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
        private Integer rankChange; // Positive = up, Negative = down
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
        private RankingInfo myRank; // Current player's rank
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response<T> {
        private Integer code;
        private String message;
        private T data;
        
        public static <T> Response<T> success(T data) {
            return Response.<T>builder().code(0).message("Success").data(data).build();
        }
        
        public static <T> Response<T> error(Integer code, String message) {
            return Response.<T>builder().code(code).message(message).build();
        }
    }
}
