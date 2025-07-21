package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.model.UserTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTaskRepository extends JpaRepository<UserTask, Long> {
    List<UserTask> findByUserId(Long userId);
    Optional<UserTask> findByUserIdAndTaskDefId(Long userId, Long taskDefId);
}