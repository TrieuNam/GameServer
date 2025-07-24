package org.SouthMillion.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HeartbeatEventDto {
    private String sessionId;
    private String roleId;
    private long timestamp;
}