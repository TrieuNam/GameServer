package com.SouthMillion.worlds_ervice.repository;

import com.SouthMillion.worlds_ervice.entity.WorldEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WorldEventRepository extends JpaRepository<WorldEvent, Long> {
    
    List<WorldEvent> findByActiveTrue();
    
    List<WorldEvent> findByEventType(WorldEvent.EventType eventType);
    
    @Query("SELECT e FROM WorldEvent e WHERE e.startTime <= :now AND e.endTime >= :now")
    List<WorldEvent> findActiveEventsAt(Instant now);
    
    @Query("SELECT e FROM WorldEvent e WHERE e.startTime > :now ORDER BY e.startTime ASC")
    List<WorldEvent> findUpcomingEvents(Instant now);
}
