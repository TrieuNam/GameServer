package com.SouthMillion.gm.repository;

import com.SouthMillion.gm.entity.GMActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GMActionLogRepository extends JpaRepository<GMActionLog, Long> {
    
    Page<GMActionLog> findByGmIdOrderByTimestampDesc(Long gmId, Pageable pageable);
    
    Page<GMActionLog> findByTargetPlayerIdOrderByTimestampDesc(String playerId, Pageable pageable);
    
    Page<GMActionLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);
    
    List<GMActionLog> findTop100ByOrderByTimestampDesc();
    
    List<GMActionLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
