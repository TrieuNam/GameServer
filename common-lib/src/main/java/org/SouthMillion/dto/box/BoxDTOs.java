package org.SouthMillion.dto.box;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class BoxDTOs {
    @Getter @Setter @Builder
    public static class InfoResp {
        private int boxLevel;
        private int boxBuyTimes;
        private long levelUpEndEpoch;
        private int levelFetchFlag;
        private int openBoxTotal;
        private boolean lastOpenIsFive;
        private Map<String,Object> pending; // null nếu không có
    }

    @Getter @Setter public static class OpenReq {
        @NotBlank private String roleId;
        @Min(1) @Max(5) private int count;        // 1 hoặc 5
        @Min(1) private int roleLevel;            // để map random_level
    }
    @Getter @Setter @Builder
    public static class OpenResp {
        private Map<String,Object> pending;       // thông tin equip vừa roll (color,level,attrs…)
        private int openBoxTotal;
        private boolean lastOpenIsFive;
        private List<Map<String,Object>> bonusItems; // challenge/fashion/... nếu có
    }

    @Getter @Setter public static class WearReq { @NotBlank private String roleId; }
    @Getter @Setter public static class SellReq { @NotBlank private String roleId; }

    @Getter @Setter public static class SimpleReq { @NotBlank private String roleId; }
    @Getter @Setter public static class QuickenReq extends SimpleReq { @Min(1) private int num; }
    @Getter @Setter public static class LevelRewardReq extends SimpleReq { @Min(1) private int idx; }

    @Getter @Setter @Builder public static class OkResp { private boolean ok; private String message; }

    // Luck Unpacking
    @Getter @Setter @Builder
    public static class LuckInfoResp {
        private long endTimestamp;
        private long receiveFlag;
        private int  openBoxNumDelta;
        private int  boxLevel;
    }
    @Getter @Setter public static class LuckReceiveReq { @NotBlank private String roleId; @Min(0) private int seq; }
}