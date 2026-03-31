package com.SouthMillion.territory_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Territory Building Entity
 * Buildings constructed in territory
 */
@Entity
@Table(name = "territory_building", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_slot", columnList = "user_id,slot_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerritoryBuilding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Building slot ID (position in territory)
     */
    @Column(name = "slot_id", nullable = false)
    private Integer slotId;
    
    /**
     * Building config ID (building type)
     */
    @Column(name = "building_id", nullable = false)
    private Integer buildingId;
    
    /**
     * Building level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Building status: 0-Empty, 1-Constructing, 2-Built, 3-Upgrading
     */
    @Column(name = "status", nullable = false)
    private Integer status;
    
    /**
     * Construction/upgrade start time
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    /**
     * Construction/upgrade finish time
     */
    @Column(name = "finish_time")
    private LocalDateTime finishTime;
    
    /**
     * Building production rate (if production building)
     */
    @Column(name = "production_rate", nullable = false)
    private Long productionRate;
    
    /**
     * Building defense contribution
     */
    @Column(name = "defense_contribution", nullable = false)
    private Long defenseContribution;
    
    /**
     * Building attack contribution
     */
    @Column(name = "attack_contribution", nullable = false)
    private Long attackContribution;
    
    /**
     * Building prosperity contribution
     */
    @Column(name = "prosperity_contribution", nullable = false)
    private Long prosperityContribution;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
