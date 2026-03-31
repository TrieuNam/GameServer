package com.SouthMillion.starmap_service.repository;

import com.SouthMillion.starmap_service.model.entity.Constellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConstellationRepository extends JpaRepository<Constellation, Long> {
    
    List<Constellation> findByUserId(Long userId);
    
    Optional<Constellation> findByUserIdAndConstellationId(Long userId, Integer constellationId);
    
    List<Constellation> findByUserIdAndIsUnlockedTrue(Long userId);
    
    List<Constellation> findByUserIdAndIsCompletedTrue(Long userId);
    
    long countByUserIdAndIsCompletedTrue(Long userId);
}
