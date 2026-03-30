package org.SouthMillion.dto.ShiZhuang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ShizhuangDTOs {

    // --------------------------------------------------------
    // Response: list of shizhuang owned by a role
    // --------------------------------------------------------
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ShizhuangListResp {
        private int totalCount;
        private List<ShizhuangInfo> items;
    }

    // --------------------------------------------------------
    // Single shizhuang info
    // --------------------------------------------------------
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ShizhuangInfo {
        private String roleId;
        private int    shizhuangId;
        private int    level;
        private int    star;
        private Boolean activated;
        private Boolean wearing;
    }

    // --------------------------------------------------------
    // Activate request
    // --------------------------------------------------------
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActivateReq {
        private String roleId;
        private int    shizhuangId;
    }

    // --------------------------------------------------------
    // Wear request
    // --------------------------------------------------------
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WearReq {
        private String roleId;
        private int    shizhuangId;
    }

    // --------------------------------------------------------
    // Level-up request
    // --------------------------------------------------------
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LevelUpReq {
        private String roleId;
        private int    shizhuangId;
    }

    // --------------------------------------------------------
    // Generic operation response
    // --------------------------------------------------------
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OperationResp {
        private Boolean ok;
        private String  message;

        public static OperationResp ok() {
            return OperationResp.builder().ok(true).message("OK").build();
        }

        public static OperationResp fail(String message) {
            return OperationResp.builder().ok(false).message(message).build();
        }
    }
}
