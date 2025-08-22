package org.SouthMillion.dto.session;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntrospectResponse {
    private boolean active;
    private String userId;
    private String username;
    private String sessionId;    // jti (access)
    private long exp;            // epoch seconds
    private String scope;        // nếu dùng
}