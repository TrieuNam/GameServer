package org.SouthMillion.dto.event.task;

import lombok.*;

import java.time.Instant;

/**
 * Task Progress Event - Published by gameplay producers to task-service.
 * Kafka topic: task.progress.update
 *
 * Producers can emit this event directly after a gameplay action is committed
 * or after normalizing raw domain events for task progress processing.
 *
 * Consumed by: task-service (TaskProgressEventConsumer)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProgressEvent {
    /** UUID — idempotency key */
    private String eventId;

    /** Player whose task progress should be updated */
    private Long roleId;

    /**
     * Task key matching TaskDefinitionConfig keys.
     * e.g. "kill_monster" | "join_guild" | "create_guild" | "arena_win" | "trial_complete"
     */
    private String taskKey;

    /** Amount to add to current progress (usually 1, except kill_monster = killCount) */
    private Integer progressDelta;

    /**
     * Source service that originally triggered the event.
     * e.g. "guild-service" | "combat" | "arena-service" | "trial-service"
     */
    private String source;

    /** When the original game action occurred */
    private Instant occurredAt;
}

