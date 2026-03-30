package com.SouthMillion.territory_service.repository;

import com.SouthMillion.territory_service.model.entity.Territory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TerritoryRepository extends JpaRepository<Territory, Long> {
    
    Optional<Territory> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
