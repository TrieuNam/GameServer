package com.SouthMillion.task_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Task {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String status;
    private Long userId;
}
