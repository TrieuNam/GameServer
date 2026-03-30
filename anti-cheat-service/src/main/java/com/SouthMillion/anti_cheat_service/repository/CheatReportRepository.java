package com.SouthMillion.anti_cheat_service.repository;

import com.SouthMillion.anti_cheat_service.entity.CheatReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CheatReportRepository extends JpaRepository<CheatReport, Long> {
    
    List<CheatReport> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<CheatReport> findByStatusOrderByCreatedAtDesc(String status);
    
    List<CheatReport> findByCheatTypeAndStatusOrderByCreatedAtDesc(String cheatType, String status);
    
    @Query("SELECT cr FROM CheatReport cr WHERE cr.severity >= :minSeverity AND cr.status = 'DETECTED' ORDER BY cr.createdAt DESC")
    List<CheatReport> findHighSeverityUnreviewedReports(Integer minSeverity);
    
    @Query("SELECT COUNT(cr) FROM CheatReport cr WHERE cr.userId = :userId AND cr.status IN ('CONFIRMED', 'BANNED') AND cr.createdAt >= :since")
    Long countConfirmedCheatsByUserSince(String userId, LocalDateTime since);
}
