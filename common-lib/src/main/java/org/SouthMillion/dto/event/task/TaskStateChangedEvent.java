package org.SouthMillion.dto.event.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.SouthMillion.dto.task.TaskStatus;

import java.time.Instant;

/**
 * Published by task-service after task progress/status is committed.
 * Consumed by websocket-server to push fresh task state to online clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStateChangedEvent {
    private String eventId;
    private Long roleId;
    private String taskKey;
    private Integer currentProgress;
    private Integer targetValue;
    private TaskStatus status;
    private Instant occurredAt;
}