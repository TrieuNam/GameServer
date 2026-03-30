package com.SouthMillion.escort_service.repository;

import com.SouthMillion.escort_service.model.entity.EscortMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EscortMissionRepository extends JpaRepository<EscortMission, Long> {
    
    List<EscortMission> findByUserId(Long userId);
    
    Optional<EscortMission> findByUserIdAndId(Long userId, Long missionId);
    
    List<EscortMission> findByUserIdAndStatus(Long userId, Integer status);
    
    List<EscortMission> findByUserIdAndStatusIn(Long userId, List<Integer> statuses);
    
    long countByUserIdAndStatus(Long userId, Integer status);
    
    @Query("SELECT em FROM EscortMission em WHERE em.userId = :userId AND em.status = 1 AND em.expiryTime < :currentTime")
    List<EscortMission> findExpiredMissions(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT em FROM EscortMission em WHERE em.userId = :userId AND em.status = 2 AND em.isRewardClaimed = false")
    List<EscortMission> findUnclaimedRewards(@Param("userId") Long userId);

    /** Find all missions across all users with a given status — used by scheduler. */
    List<EscortMission> findAllByStatus(Integer status);

    /** Find distinct userIds that have at least one mission with a given status. */
    @Query("SELECT DISTINCT em.userId FROM EscortMission em WHERE em.status = :status")
    List<Long> findDistinctUserIdsByStatus(@Param("status") Integer status);
}
