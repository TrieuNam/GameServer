package org.SouthMillion.dto.session;

import jakarta.validation.constraints.*;
import lombok.*;

public class SessionDTOs {

    @Data
    public static class LoginRequest {
        @Min(1) private int spid;
        @NotBlank private String deviceId;   // GET compat: 'device'
        @NotBlank private String account;    // GET compat: 'userId'
        private long timestamp;
        @NotBlank private String loginStr;   // GET compat: 'sign'
        private int server;
        private Boolean kickOld;             // optional override
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class LoginResponse {
        private String token;
        private String userId;
        private String sessionId;
        private long expireAt;     // epoch seconds
        private boolean kickOld;
    }

    @Data
    public static class ValidateResponse {
        private boolean valid;
        private String userId;
        private String sessionId;
        private String roleId;   // nullable
        private long expireAt;   // epoch seconds (từ PTTL)
    }

    @Data
    public static class HeartbeatRequest {
        @NotBlank private String token;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class HeartbeatResponse {
        private boolean ok;
        private int extendSeconds;
        private long expireAt;
    }

    @Data
    public static class BindRoleRequest {
        @NotBlank private String token;
        @NotBlank private String roleId;
    }

    @Data
    public static class InvalidateRequest {
        @NotBlank private String token;
        private String reason;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class BriefResponse {
        private boolean ok;
        private String userId;
        private String roleId;
        private String sessionId;
        private long expireAt;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class OkResponse {
        private boolean ok;
    }
}