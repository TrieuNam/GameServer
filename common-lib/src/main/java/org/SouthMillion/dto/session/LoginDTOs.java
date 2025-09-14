package org.SouthMillion.dto.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

/**
 * DTOs cho session-service
 * - Tương thích Spring Boot 3.5.3, Java 21
 * - Dùng trong các endpoint:
 *   POST /api/session/login
 *   POST /api/session/refresh
 *   POST /api/session/heartbeat  (no body)
 *   GET  /api/session/time
 *   POST /api/session/introspect
 */
public class LoginDTOs {

    // ===== Requests =====

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true) // cho phép client cũ gửi thêm field (loginTime, loginStr, ...)
    public static class LoginReq {
        @NotBlank
        @Size(max = 64)
        private String username;

        @NotBlank
        @Size(max = 1000)
        private String password;

        /** Tuỳ chọn, giúp theo dõi phiên theo thiết bị */
        @Size(max = 128)
        private String deviceId;
    }

    @Data
    public static class RefreshReq {
        @NotBlank
        private String refreshToken;
    }

    // ===== Common =====

    @Builder
    @Data
    @NoArgsConstructor  // <<< BẮT BUỘC để Jackson tạo instance khi dùng @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TokenPair {
        private String accessToken;
        private long   accessExpiresAt;   // epoch seconds
        private String refreshToken;
        private long   refreshExpiresAt;  // epoch seconds
        private String sessionId;
    }

    // ===== Responses =====

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LoginResp {
        private String userId;
        private String account;       // thường là username
        private String sessionId;

        private String accessToken;   // Bearer
        private long   expiresAt;     // epoch seconds (hết hạn access token)
        private String refreshToken;  // có thể rotate -> khác sau mỗi lần refresh
        private String tokenType;     // "Bearer"
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RefreshResp {
        private String userId;
        private String account;
        private String sessionId;

        private String accessToken;
        private long   expiresAt;     // epoch seconds
        private String refreshToken;  // nếu rotate-refresh=true sẽ là token mới
        private String tokenType;     // "Bearer"
    }

    @Builder
    @Data
    @NoArgsConstructor  // <<< BẮT BUỘC để Jackson tạo instance
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntrospectResp {
        private boolean active;
        private String  reason;     // invalid_token | revoked | user_inactive
        private String  userId;
        private String  account;
        private String  sessionId;
        private long    exp;        // epoch seconds (hạn của access token)
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeResp {
        private long serverTime;    // epoch millis
    }
}