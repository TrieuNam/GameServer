package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.model.TaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskProgressRepository extends JpaRepository<TaskProgress, Long> {
    Optional<TaskProgress> findByUserIdAndTaskCodeAndTargetId(String userId, String taskCode, int targetId);
    Optional<TaskProgress> findByUserIdAndTargetId(String userId, int targetId);
}