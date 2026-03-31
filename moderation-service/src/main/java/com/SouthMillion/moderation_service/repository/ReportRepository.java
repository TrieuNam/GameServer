package com.SouthMillion.moderation_service.repository;

import com.SouthMillion.moderation_service.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReportedUserIdOrderByCreatedAtDesc(String reportedUserId);
    List<Report> findByStatusOrderByCreatedAtDesc(String status);
    Long countByReportedUserIdAndStatus(String reportedUserId, String status);
}
