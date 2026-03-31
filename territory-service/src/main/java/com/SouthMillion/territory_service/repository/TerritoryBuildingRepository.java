package com.SouthMillion.territory_service.repository;

import com.SouthMillion.territory_service.model.entity.TerritoryBuilding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TerritoryBuildingRepository extends JpaRepository<TerritoryBuilding, Long> {
    
    List<TerritoryBuilding> findByUserId(Long userId);
    
    Optional<TerritoryBuilding> findByUserIdAndSlotId(Long userId, Integer slotId);
    
    List<TerritoryBuilding> findByUserIdAndStatus(Long userId, Integer status);
    
    boolean existsByUserIdAndSlotId(Long userId, Integer slotId);
    
    @Query("SELECT tb FROM TerritoryBuilding tb WHERE tb.userId = :userId AND tb.finishTime IS NOT NULL AND tb.finishTime <= :currentTime AND (tb.status = 1 OR tb.status = 3)")
    List<TerritoryBuilding> findCompletedConstructions(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT SUM(tb.productionRate) FROM TerritoryBuilding tb WHERE tb.userId = :userId AND tb.status = 2")
    Long getTotalProductionRate(@Param("userId") Long userId);
    
    @Query("SELECT SUM(tb.defenseContribution) FROM TerritoryBuilding tb WHERE tb.userId = :userId AND tb.status = 2")
    Long getTotalDefense(@Param("userId") Long userId);
    
    @Query("SELECT SUM(tb.attackContribution) FROM TerritoryBuilding tb WHERE tb.userId = :userId AND tb.status = 2")
    Long getTotalAttack(@Param("userId") Long userId);
    
    @Query("SELECT SUM(tb.prosperityContribution) FROM TerritoryBuilding tb WHERE tb.userId = :userId AND tb.status = 2")
    Long getTotalProsperity(@Param("userId") Long userId);
}
