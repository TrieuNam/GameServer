package org.SouthMillion.dto.session;

import lombok.Data;

@Data
public class HeartbeatRequest {
    private String sessionId;
    private String roleId;
}
