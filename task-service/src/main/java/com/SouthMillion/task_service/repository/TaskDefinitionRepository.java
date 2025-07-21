package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, Long> {
}
