package com.SouthMillion.starmap_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Star Entity
 * Individual star in constellation system
 * C++ Source: gameworld/other/starmap/
 */
@Entity
@Table(name = "star", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_star", columnList = "user_id,star_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Star {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Star config ID (position in constellation)
     */
    @Column(name = "star_id", nullable = false)
    private Integer starId;
    
    /**
     * Star level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Is star activated/lit
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * Star energy accumulated
     */
    @Column(name = "energy", nullable = false)
    private Long energy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
