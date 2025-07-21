package com.SouthMillion.task_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long taskDefId;
    private int progress;
    private int status; // 0: doing, 1: done, 2: claimed
    private Long updateTime;
}