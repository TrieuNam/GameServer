package com.SouthMillion.trial_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Trial Record Entity
 * Tracks user's progress in trial/challenge stages
 */
@Entity
@Table(name = "trial_record", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_trial", columnList = "user_id,trial_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrialRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Trial/Challenge config ID
     */
    @Column(name = "trial_id", nullable = false)
    private Integer trialId;
    
    /**
     * Current stage/floor reached
     */
    @Column(name = "current_stage", nullable = false)
    private Integer currentStage;
    
    /**
     * Best stage/floor ever reached
     */
    @Column(name = "best_stage", nullable = false)
    private Integer bestStage;
    
    /**
     * Total number of attempts
     */
    @Column(name = "total_attempts", nullable = false)
    private Integer totalAttempts;
    
    /**
     * Attempts remaining today
     */
    @Column(name = "remaining_attempts", nullable = false)
    private Integer remainingAttempts;
    
    /**
     * Is stage currently in progress
     */
    @Column(name = "is_in_progress", nullable = false)
    private Boolean isInProgress;
    
    /**
     * Score for current stage
     */
    @Column(name = "current_score", nullable = false)
    private Long currentScore;
    
    /**
     * Best score ever achieved
     */
    @Column(name = "best_score", nullable = false)
    private Long bestScore;
    
    /**
     * Number of stars earned (0-3)
     */
    @Column(name = "stars", nullable = false)
    private Integer stars;
    
    /**
     * Completion time in seconds (for speed records)
     */
    @Column(name = "completion_time")
    private Integer completionTime;
    
    /**
     * Best completion time
     */
    @Column(name = "best_time")
    private Integer bestTime;
    
    /**
     * Last reset date
     */
    @Column(name = "last_reset_date")
    private LocalDateTime lastResetDate;
    
    /**
     * Rewards claimed for stages (bit flags)
     */
    @Column(name = "rewards_claimed", nullable = false)
    private Long rewardsClaimed;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
