package com.SouthMillion.escort_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Escort Statistics Entity
 * Player's overall escort mission statistics
 */
@Entity
@Table(name = "escort_stats", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscortStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    /**
     * Total missions completed
     */
    @Column(name = "total_completed", nullable = false)
    private Integer totalCompleted;
    
    /**
     * Total missions failed
     */
    @Column(name = "total_failed", nullable = false)
    private Integer totalFailed;
    
    /**
     * Missions completed today
     */
    @Column(name = "daily_completed", nullable = false)
    private Integer dailyCompleted;
    
    /**
     * Daily mission limit
     */
    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;
    
    /**
     * Free refreshes remaining today
     */
    @Column(name = "free_refreshes", nullable = false)
    private Integer freeRefreshes;
    
    /**
     * Total refreshes used today
     */
    @Column(name = "total_refreshes_today", nullable = false)
    private Integer totalRefreshesToday;
    
    /**
     * Last daily reset date
     */
    @Column(name = "last_reset_date")
    private LocalDateTime lastResetDate;
    
    /**
     * Best quality completed (1-5)
     */
    @Column(name = "best_quality", nullable = false)
    private Integer bestQuality;
    
    /**
     * Perfect completions (no attacks survived)
     */
    @Column(name = "perfect_completions", nullable = false)
    private Integer perfectCompletions;
    
    /**
     * Total distance traveled
     */
    @Column(name = "total_distance", nullable = false)
    private Long totalDistance;
    
    /**
     * Total gold earned from escorts
     */
    @Column(name = "total_gold_earned", nullable = false)
    private Long totalGoldEarned;
    
    /**
     * Total exp earned from escorts
     */
    @Column(name = "total_exp_earned", nullable = false)
    private Long totalExpEarned;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── PB_SCEscortRoleInfo (9622) fields ───────────────────────────────────
    /** SC:9622 ship — current ship level */
    @Column(name = "current_ship_level", nullable = false)
    private Integer currentShipLevel = 1;

    /** SC:9622 escort_count — times completed as escort */
    @Column(name = "escort_count", nullable = false)
    private Integer escortCount = 0;

    /** SC:9622 intercept_count — times intercepted others */
    @Column(name = "intercept_count", nullable = false)
    private Integer interceptCount = 0;

    /** SC:9622 help_count — times helped others (put out fire) */
    @Column(name = "help_count", nullable = false)
    private Integer helpCount = 0;

    /** SC:9622 reward_index — number of level rewards claimed */
    @Column(name = "rewards_claimed", nullable = false)
    private Integer rewardsClaimed = 0;
}
