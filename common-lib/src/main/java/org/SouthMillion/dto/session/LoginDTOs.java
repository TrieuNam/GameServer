package org.SouthMillion.dto.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

public class LoginDTOs {

    @Data
    public static class LoginReq {
        @NotBlank
        @Size(max = 64)
        private String username;

        @NotBlank @Size(max = 1000)
        private String password;
    }

    @Data
    public static class RefreshReq {
        @NotBlank
        private String refreshToken;
    }

    @Builder
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TokenPair {
        private String accessToken;
        private long   accessExpiresAt;  // epoch seconds
        private String refreshToken;
        private long   refreshExpiresAt; // epoch seconds
        private String sessionId;
    }

    @Builder
    @Data
    @NoArgsConstructor         // <<< BẮT BUỘC để Jackson tạo instance
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntrospectResp {
        private boolean active;
        private String  reason;     // invalid_token | revoked | user_inactive
        private String  userId;
        private String  account;
        private String  sessionId;
        private Instant exp;
    }
}