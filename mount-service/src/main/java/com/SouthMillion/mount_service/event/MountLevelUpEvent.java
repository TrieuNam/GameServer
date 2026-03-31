package com.SouthMillion.mount_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MountLevelUpEvent {
    private String eventId;
    private String userId;
    private Integer mountId;
    private Integer mountIndex;
    private Integer oldLevel;
    private Integer newLevel;
    private Integer levelsGained;
    private Long expGained;
    private Long timestamp;
    private String source = "mount-service";
}
