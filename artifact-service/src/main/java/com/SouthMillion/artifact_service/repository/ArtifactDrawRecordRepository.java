package com.SouthMillion.artifact_service.repository;

import com.SouthMillion.artifact_service.model.entity.ArtifactDrawRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ArtifactDrawRecordRepository extends JpaRepository<ArtifactDrawRecord, Long> {
    
    /**
     * Get recent draw records for a user (last 100 records)
     */
    List<ArtifactDrawRecord> findTop100ByUserIdOrderByDrawTimestampDesc(Long userId);
    
    /**
     * Get draw records within date range
     */
    List<ArtifactDrawRecord> findByUserIdAndDrawTimestampBetween(
            Long userId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Count total draws by user
     */
    long countByUserId(Long userId);
    
    /**
     * Count draws of specific type (for pity counter)
     */
    @Query("SELECT COUNT(d) FROM ArtifactDrawRecord d WHERE d.userId = ?1 AND d.quality >= ?2")
    long countHighQualityDraws(Long userId, Integer minQuality);
}
