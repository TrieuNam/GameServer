package org.SouthMillion.dto.task;

import lombok.Data;

@Data
public class TaskClaimRequest {
    private Long userId;
    private Long taskDefId;
}
