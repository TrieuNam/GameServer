package com.SouthMillion.starmap_service.repository;

import com.SouthMillion.starmap_service.model.entity.Star;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StarRepository extends JpaRepository<Star, Long> {
    
    List<Star> findByUserId(Long userId);
    
    Optional<Star> findByUserIdAndStarId(Long userId, Integer starId);
    
    List<Star> findByUserIdAndIsActiveTrue(Long userId);
    
    long countByUserIdAndIsActiveTrue(Long userId);
}
