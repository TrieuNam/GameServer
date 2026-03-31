package com.SouthMillion.admin.repository;

import com.SouthMillion.admin.entity.AdminActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    
    Page<AdminActionLog> findByAdminIdOrderByTimestampDesc(Long adminId, Pageable pageable);
    
    Page<AdminActionLog> findByTargetPlayerIdOrderByTimestampDesc(String targetPlayerId, Pageable pageable);
    
    List<AdminActionLog> findTop100ByOrderByTimestampDesc();
}
