package com.SouthMillion.report_service.repository;

import com.SouthMillion.report_service.entity.ReportEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReportEventRepository extends JpaRepository<ReportEvent, Long> {
    List<ReportEvent> findByType(int type);
    
    List<ReportEvent> findByDeviceId(String deviceId);
    
    List<ReportEvent> findByEventTimeBetween(long startTime, long endTime);
    
    long countByCreatedAtAfter(Instant createdAt);
    
    @Query("SELECT COUNT(DISTINCT r.deviceId) FROM ReportEvent r WHERE r.createdAt > :createdAt")
    long countDistinctDeviceIdByCreatedAtAfter(@Param("createdAt") Instant createdAt);
    
    @Query("SELECT COUNT(DISTINCT r.sessionId) FROM ReportEvent r WHERE r.createdAt > :createdAt")
    long countDistinctSessionIdByCreatedAtAfter(@Param("createdAt") Instant createdAt);
    
    long countByType(int type);
}