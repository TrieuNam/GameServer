package com.SouthMillion.anti_cheat_service.repository;

import com.SouthMillion.anti_cheat_service.entity.SuspiciousActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, Long> {
    
    List<SuspiciousActivity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<SuspiciousActivity> findByIsResolvedOrderByCreatedAtDesc(Boolean isResolved);
    
    @Query("SELECT sa FROM SuspiciousActivity sa WHERE sa.suspicionScore >= :threshold AND sa.isResolved = false ORDER BY sa.suspicionScore DESC")
    List<SuspiciousActivity> findHighSuspicionActivities(Integer threshold);
    
    @Query("SELECT SUM(sa.suspicionScore) FROM SuspiciousActivity sa WHERE sa.userId = :userId AND sa.createdAt >= :since AND sa.isResolved = false")
    Long calculateTotalSuspicionScore(String userId, LocalDateTime since);
}
