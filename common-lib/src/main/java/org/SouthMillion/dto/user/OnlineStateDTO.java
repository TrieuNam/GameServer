package org.SouthMillion.dto.user;

import lombok.Data;

@Data
public class OnlineStateDTO {
    private Boolean online;

    public OnlineStateDTO(Boolean online) {
        this.online = online;
    }
}
