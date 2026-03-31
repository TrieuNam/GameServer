package com.SouthMillion.task_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "task_progress_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProgressEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "task_key", nullable = false, length = 64)
    private String taskKey;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}