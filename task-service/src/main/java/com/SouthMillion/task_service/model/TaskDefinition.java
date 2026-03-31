package com.SouthMillion.task_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_definition")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDefinition {
    @Id
    private Long id;
    private String name;
    private String description;
    private int rewardType;
    private long rewardValue;
}
