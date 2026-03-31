package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.TaskProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskProgressRepository extends JpaRepository<TaskProgressEntity, Long> {
    Optional<TaskProgressEntity> findByPlayerIdAndTaskKey(Long playerId, String taskKey);

    List<TaskProgressEntity> findAllByPlayerId(Long playerId);
    
    // Alias for findAllByPlayerId
    default List<TaskProgressEntity> findByPlayerId(Long playerId) {
        return findAllByPlayerId(playerId);
    }

    List<TaskProgressEntity> findByPlayerIdAndStatus(Long playerId, org.SouthMillion.dto.task.TaskStatus status);

    @Modifying
    @Query(value = """
        UPDATE task_progress
           SET status = 'CLAIMED', last_update = :now
         WHERE player_id = :playerId
           AND task_key = :taskKey
           AND status = 'COMPLETED'
        """, nativeQuery = true)
    int markClaimedIfCompleted(@Param("playerId") Long playerId,
                               @Param("taskKey") String taskKey,
                               @Param("now") Instant now);
}