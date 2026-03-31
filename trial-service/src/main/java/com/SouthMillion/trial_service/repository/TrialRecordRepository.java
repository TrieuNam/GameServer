package com.SouthMillion.trial_service.repository;

import com.SouthMillion.trial_service.model.entity.TrialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrialRecordRepository extends JpaRepository<TrialRecord, Long> {
    
    List<TrialRecord> findByUserId(Long userId);
    
    Optional<TrialRecord> findByUserIdAndTrialId(Long userId, Integer trialId);
    
    List<TrialRecord> findByUserIdAndIsInProgressTrue(Long userId);
    
    boolean existsByUserIdAndTrialId(Long userId, Integer trialId);
    
    @Query("SELECT tr FROM TrialRecord tr WHERE tr.userId = :userId AND tr.bestStage >= :minStage")
    List<TrialRecord> findByUserIdAndMinStage(@Param("userId") Long userId, @Param("minStage") Integer minStage);
    
    @Query("SELECT tr FROM TrialRecord tr WHERE tr.userId = :userId AND tr.lastResetDate < :resetDate")
    List<TrialRecord> findByUserIdAndNeedsReset(@Param("userId") Long userId, @Param("resetDate") LocalDateTime resetDate);
    
    @Query("SELECT SUM(tr.bestScore) FROM TrialRecord tr WHERE tr.userId = :userId")
    Long getTotalScore(@Param("userId") Long userId);
}
