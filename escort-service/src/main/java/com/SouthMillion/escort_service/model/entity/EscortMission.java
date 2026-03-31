package com.SouthMillion.escort_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Escort Mission Entity
 * Active escort missions for players
 */
@Entity
@Table(name = "escort_mission", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "user_id,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscortMission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Mission config ID (route/type)
     */
    @Column(name = "mission_id", nullable = false)
    private Integer missionId;
    
    /**
     * Mission quality/tier: 1-White, 2-Green, 3-Blue, 4-Purple, 5-Orange
     */
    @Column(name = "quality", nullable = false)
    private Integer quality;
    
    /**
     * Mission status: 0-Available, 1-InProgress, 2-Completed, 3-Failed, 4-Expired
     */
    @Column(name = "status", nullable = false)
    private Integer status;
    
    /**
     * Mission start time
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    /**
     * Mission deadline/expiry time
     */
    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;
    
    /**
     * Mission completion time
     */
    @Column(name = "completion_time")
    private LocalDateTime completionTime;
    
    /**
     * Distance to travel (in meters/units)
     */
    @Column(name = "distance", nullable = false)
    private Integer distance;
    
    /**
     * Current progress (distance covered)
     */
    @Column(name = "progress", nullable = false)
    private Integer progress;
    
    /**
     * Number of attacks/interceptions survived
     */
    @Column(name = "attacks_survived", nullable = false)
    private Integer attacksSurvived;
    
    /**
     * Base reward gold
     */
    @Column(name = "reward_gold", nullable = false)
    private Long rewardGold;
    
    /**
     * Base reward exp
     */
    @Column(name = "reward_exp", nullable = false)
    private Long rewardExp;
    
    /**
     * Bonus multiplier (for perfect completion)
     */
    @Column(name = "bonus_multiplier", nullable = false)
    private Double bonusMultiplier;
    
    /**
     * Is reward claimed
     */
    @Column(name = "is_reward_claimed", nullable = false)
    private Boolean isRewardClaimed;
    
    /**
     * Source location ID
     */
    @Column(name = "source_location", nullable = false)
    private Integer sourceLocation;
    
    /**
     * Destination location ID
     */
    @Column(name = "destination_location", nullable = false)
    private Integer destinationLocation;
    
    /**
     * Escort cargo/NPC type
     */
    @Column(name = "cargo_type", nullable = false)
    private Integer cargoType;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** PB_SCEscortShipData.ship_key — unique channel key for intercept/help targeting */
    @Column(name = "ship_key", nullable = false)
    private Integer shipKey = 0;
}
