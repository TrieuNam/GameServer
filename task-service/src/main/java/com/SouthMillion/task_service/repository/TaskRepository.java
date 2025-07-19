package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {}