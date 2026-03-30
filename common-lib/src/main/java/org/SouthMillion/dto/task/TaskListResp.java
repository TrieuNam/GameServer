package org.SouthMillion.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListResp {
    private List<TaskDTO> tasks;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer claimedTasks;
}
