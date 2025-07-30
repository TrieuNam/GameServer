package com.SouthMillion.task_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "completed_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletedTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private Integer taskId;

    // ... thêm các trường khác nếu cần
}