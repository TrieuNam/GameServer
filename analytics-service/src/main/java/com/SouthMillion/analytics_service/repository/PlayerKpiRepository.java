package com.SouthMillion.analytics_service.repository;

import com.SouthMillion.analytics_service.entity.PlayerKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerKpiRepository extends JpaRepository<PlayerKpi, Long> {
    
    Optional<PlayerKpi> findByPlayerIdAndDate(Long playerId, LocalDateTime date);
    
    List<PlayerKpi> findByPlayerIdAndDateBetween(
        Long playerId, 
        LocalDateTime start, 
        LocalDateTime end
    );
    
    @Query("SELECT k FROM PlayerKpi k WHERE k.date >= ?1 ORDER BY k.totalSpent DESC")
    List<PlayerKpi> findTopSpenders(LocalDateTime since);
    
    @Query("SELECT k FROM PlayerKpi k WHERE k.date >= ?1 ORDER BY k.sessionDuration DESC")
    List<PlayerKpi> findMostActiveUsers(LocalDateTime since);
}
