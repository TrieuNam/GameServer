package org.SouthMillion.dto.task;

import lombok.Data;

@Data
public class TaskClaimRequest {
    private String userId;
    private Long taskDefId;
}
