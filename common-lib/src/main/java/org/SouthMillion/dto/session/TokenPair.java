package org.SouthMillion.dto.session;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenPair {
    private String accessToken;
    private long   accessExpiresAt;   // epoch seconds
    private String refreshToken;
    private long   refreshExpiresAt;  // epoch seconds
    private String sessionId;         // jti của refresh
}