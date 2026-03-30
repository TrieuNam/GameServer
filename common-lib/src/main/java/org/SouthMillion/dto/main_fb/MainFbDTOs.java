package org.SouthMillion.dto.main_fb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTOs for Main Dungeon/Copy service
 */
public class MainFbDTOs {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressResp {
        private List<StageProgress> progresses;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageProgress {
        private Integer stage;
        private Integer level;
        private Integer bestScore;
        private Integer clearCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnterStageReq {
        private String playerId;
        private Integer stage;
        private Integer level;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnterStageResp {
        private String battleId;
        private Long seed;
        private Long expireAt;
        private Integer cost;
        private String message;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinishStageReq {
        private String playerId;
        private String battleId;
        private Integer stage;
        private Integer level;
        private Integer score;
        private Boolean victory;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SweepReq {
        private String playerId;
        private Integer stage;
        private Integer level;
        private Integer times;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SweepResp {
        private List<RewardItem> rewards;
        private Integer sweepCount;
        private String message;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardItem {
        private Long itemId;
        private Integer count;
    }
}
