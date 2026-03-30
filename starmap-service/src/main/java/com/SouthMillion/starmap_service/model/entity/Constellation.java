package com.SouthMillion.starmap_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Constellation Entity
 * Group of stars forming constellation pattern
 */
@Entity
@Table(name = "constellation", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_constellation", columnList = "user_id,constellation_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Constellation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Constellation config ID
     */
    @Column(name = "constellation_id", nullable = false)
    private Integer constellationId;
    
    /**
     * Constellation level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Is constellation unlocked
     */
    @Column(name = "is_unlocked", nullable = false)
    private Boolean isUnlocked;
    
    /**
     * Is constellation completed (all stars lit)
     */
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;
    
    /**
     * Number of stars activated
     */
    @Column(name = "stars_activated", nullable = false)
    private Integer starsActivated;
    
    /**
     * Total stars in constellation
     */
    @Column(name = "total_stars", nullable = false)
    private Integer totalStars;
    
    /**
     * Constellation power/blessing
     */
    @Column(name = "power", nullable = false)
    private Long power;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
