package com.SouthMillion.territory_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Territory Entity
 * Main territory/base owned by player
 */
@Entity
@Table(name = "territory", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Territory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    /**
     * Territory config ID (type/location)
     */
    @Column(name = "territory_id", nullable = false)
    private Integer territoryId;
    
    /**
     * Territory level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Territory name (customizable)
     */
    @Column(name = "name", length = 50)
    private String name;
    
    /**
     * Prosperity/development score
     */
    @Column(name = "prosperity", nullable = false)
    private Long prosperity;
    
    /**
     * Defense rating
     */
    @Column(name = "defense_rating", nullable = false)
    private Long defenseRating;
    
    /**
     * Attack rating
     */
    @Column(name = "attack_rating", nullable = false)
    private Long attackRating;
    
    /**
     * Gold production per hour
     */
    @Column(name = "gold_production", nullable = false)
    private Long goldProduction;
    
    /**
     * Resource production per hour (generic resource)
     */
    @Column(name = "resource_production", nullable = false)
    private Long resourceProduction;
    
    /**
     * Accumulated gold (not collected yet)
     */
    @Column(name = "accumulated_gold", nullable = false)
    private Long accumulatedGold;
    
    /**
     * Accumulated resources (not collected yet)
     */
    @Column(name = "accumulated_resources", nullable = false)
    private Long accumulatedResources;
    
    /**
     * Last production time (for calculating accumulated resources)
     */
    @Column(name = "last_production_time", nullable = false)
    private LocalDateTime lastProductionTime;
    
    /**
     * Max gold storage capacity
     */
    @Column(name = "max_gold_storage", nullable = false)
    private Long maxGoldStorage;
    
    /**
     * Max resource storage capacity
     */
    @Column(name = "max_resource_storage", nullable = false)
    private Long maxResourceStorage;
    
    /**
     * Number of building slots
     */
    @Column(name = "building_slots", nullable = false)
    private Integer buildingSlots;
    
    /**
     * Appearance/skin ID
     */
    @Column(name = "appearance_id", nullable = false)
    private Integer appearanceId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
