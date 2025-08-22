package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.TaskProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskProgressRepository extends JpaRepository<TaskProgressEntity, Long> {
    Optional<TaskProgressEntity> findByPlayerIdAndTaskKey(String playerId, String taskKey);

    List<TaskProgressEntity> findAllByPlayerId(String playerId);
}