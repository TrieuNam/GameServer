package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.TaskProgressEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface TaskProgressEventRepository extends JpaRepository<TaskProgressEventEntity, String> {

	@Transactional
	long deleteByProcessedAtBefore(Instant threshold);
}