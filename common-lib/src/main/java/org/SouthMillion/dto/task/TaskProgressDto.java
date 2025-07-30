package org.SouthMillion.dto.task;

import lombok.Data;

@Data
public class TaskProgressDto {
    private String userId;
    private Integer taskId;
    private int progress;
    private int status;
    private Long updateTime;
}