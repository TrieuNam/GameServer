package org.SouthMillion.dto.task;

import lombok.Data;

@Data
public class TaskProgressDto {
    private Long userId;
    private Long taskDefId;
    private int progress;
    private int status;
    private Long updateTime;
}