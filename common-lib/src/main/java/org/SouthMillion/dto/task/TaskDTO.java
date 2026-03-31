package org.SouthMillion.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private String taskKey;
    private String taskName;
    private String description;
    private Integer targetValue;
    private Integer currentProgress;
    private Integer legacyConditionType;
    private TaskStatus status;
    private Instant lastUpdate;
    
    // Reward info
    private Integer goldReward;
    private Integer expReward;
    private String itemRewards;
}
