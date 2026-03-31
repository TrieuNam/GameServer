package com.SouthMillion.analytics_service.repository;

import com.SouthMillion.analytics_service.entity.PlayerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PlayerEventRepository extends JpaRepository<PlayerEvent, Long> {
    
    List<PlayerEvent> findByPlayerIdAndEventTimeBetween(
        Long playerId, 
        LocalDateTime start, 
        LocalDateTime end
    );
    
    List<PlayerEvent> findByPlayerIdAndEventType(Long playerId, String eventType);
    
    @Query("SELECT COUNT(e) FROM PlayerEvent e WHERE e.playerId = ?1 AND e.eventType = ?2")
    Long countByPlayerIdAndEventType(Long playerId, String eventType);
    
    @Query("SELECT e FROM PlayerEvent e WHERE e.eventTime >= ?1 ORDER BY e.eventTime DESC")
    List<PlayerEvent> findRecentEvents(LocalDateTime since);
}
