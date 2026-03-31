package com.SouthMillion.mount_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Mount Entity
 * Represents a player's mount/cavalry
 * 
 * C++ Source: gameworld/other/mount/
 * Config: harness.json
 * DB Table: harness_list
 */
@Entity
@Table(name = "mount", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_mount", columnList = "user_id,mount_index")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User ID (owner)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Mount index/slot (0-based)
     */
    @Column(name = "mount_index", nullable = false)
    private Integer mountIndex;
    
    /**
     * Mount config ID from harness.json
     */
    @Column(name = "mount_id", nullable = false)
    private Integer mountId;
    
    /**
     * Mount level (affects stats)
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Mount grade/quality (affects stats multiplier)
     */
    @Column(name = "grade", nullable = false)
    private Integer grade;
    
    /**
     * Current experience points
     */
    @Column(name = "exp", nullable = false)
    private Long exp;
    
    /**
     * Is mount activated/unlocked
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * Is currently equipped/riding
     */
    @Column(name = "is_equipped", nullable = false)
    private Boolean isEquipped;
    
    /**
     * Current appearance ID (cosmetic)
     */
    @Column(name = "appearance_id")
    private Integer appearanceId;
    
    /**
     * Current skin ID
     */
    @Column(name = "skin_id")
    private Integer skinId;
    
    /**
     * Skin level (upgrade level)
     */
    @Column(name = "skin_level", nullable = false)
    private Integer skinLevel;
    
    /**
     * Exploration progress/points
     */
    @Column(name = "explore_progress", nullable = false)
    private Long exploreProgress;
    
    /**
     * Harness slot 1 item ID
     */
    @Column(name = "harness_slot_1")
    private Integer harnessSlot1;
    
    /**
     * Harness slot 2 item ID
     */
    @Column(name = "harness_slot_2")
    private Integer harnessSlot2;
    
    /**
     * Harness slot 3 item ID
     */
    @Column(name = "harness_slot_3")
    private Integer harnessSlot3;
    
    /**
     * Harness slot 4 item ID
     */
    @Column(name = "harness_slot_4")
    private Integer harnessSlot4;
    
    /**
     * Star level (additional upgrade system)
     */
    @Column(name = "star_level", nullable = false)
    private Integer starLevel;
    
    /**
     * Lock flag for entry system (bitmask)
     */
    @Column(name = "lock_flag", nullable = false)
    private Integer lockFlag;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
