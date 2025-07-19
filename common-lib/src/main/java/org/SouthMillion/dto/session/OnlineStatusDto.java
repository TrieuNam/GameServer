package org.SouthMillion.dto.session;

import lombok.Data;

@Data
public  class OnlineStatusDto {
    private Integer roleId;
    private boolean online;
    private String sessionId;
    private Long lastActive;
}
