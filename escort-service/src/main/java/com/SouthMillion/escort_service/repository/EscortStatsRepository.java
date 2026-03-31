package com.SouthMillion.escort_service.repository;

import com.SouthMillion.escort_service.model.entity.EscortStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EscortStatsRepository extends JpaRepository<EscortStats, Long> {
    
    Optional<EscortStats> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
