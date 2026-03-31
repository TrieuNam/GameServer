package com.SouthMillion.anti_cheat_service.repository;

import com.SouthMillion.anti_cheat_service.entity.PlayerBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayerBehaviorRepository extends JpaRepository<PlayerBehavior, Long> {
    
    List<PlayerBehavior> findByUserIdAndMetricTypeOrderByRecordedAtDesc(String userId, String metricType);
    
    @Query("SELECT pb FROM PlayerBehavior pb WHERE pb.userId = :userId AND pb.recordedAt >= :since ORDER BY pb.recordedAt DESC")
    List<PlayerBehavior> findRecentBehaviorsByUserId(String userId, LocalDateTime since);
    
    @Query("SELECT pb FROM PlayerBehavior pb WHERE pb.isAnomaly = true AND pb.recordedAt >= :since ORDER BY pb.deviation DESC")
    List<PlayerBehavior> findRecentAnomalies(LocalDateTime since);
    
    @Query("SELECT COUNT(pb) FROM PlayerBehavior pb WHERE pb.userId = :userId AND pb.isAnomaly = true AND pb.recordedAt >= :since")
    Long countAnomaliesByUserSince(String userId, LocalDateTime since);
}
