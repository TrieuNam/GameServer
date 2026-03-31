package com.SouthMillion.task_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.SouthMillion.dto.task.TaskStatus;

import java.time.Instant;

@Entity
@Table(name="task_progress",
        uniqueConstraints = @UniqueConstraint(name="uq_player_task", columnNames = {"player_id","task_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="player_id", nullable = false)
    private Long playerId;

    @Column(name="task_key", nullable = false, length = 64)
    private String taskKey;

    @Column(name="progress_value", nullable = false)
    private Integer progressValue;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private TaskStatus status;

    @Column(name="last_update", nullable = false)
    private Instant lastUpdate;
}