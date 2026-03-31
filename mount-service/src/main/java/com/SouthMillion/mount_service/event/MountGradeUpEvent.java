package com.SouthMillion.mount_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MountGradeUpEvent {
    private String eventId;
    private String userId;
    private Integer mountId;
    private Integer mountIndex;
    private Integer oldGrade;
    private Integer newGrade;
    private Integer mountLevel;
    private Long timestamp;
    private String source = "mount-service";
}
