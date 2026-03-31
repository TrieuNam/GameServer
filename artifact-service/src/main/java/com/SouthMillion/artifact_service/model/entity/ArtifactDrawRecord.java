package com.SouthMillion.artifact_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Artifact Draw Record Entity
 * Records artifact gacha/draw history for players
 * 
 * Used by ShenQi Draw (op=8) and GetRecords (op=9)
 * DB Table: artifact_draw_record
 */
@Entity
@Table(name = "artifact_draw_record", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_timestamp", columnList = "user_id,draw_timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactDrawRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User ID (who drew)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Draw type (1=single draw, 10=10-pull)
     */
    @Column(name = "draw_type", nullable = false)
    private Integer drawType;
    
    /**
     * Artifact ID obtained from gacha
     */
    @Column(name = "artifact_id", nullable = false)
    private Integer artifactId;
    
    /**
     * Artifact quality/rarity (1=common, 2=rare, 3=epic, 4=legendary, 5=mythic)
     */
    @Column(name = "quality", nullable = false)
    private Integer quality;
    
    /**
     * Is this a guaranteed/pity draw
     */
    @Column(name = "is_guaranteed", nullable = false)
    private Boolean isGuaranteed;
    
    /**
     * Draw timestamp
     */
    @CreationTimestamp
    @Column(name = "draw_timestamp", nullable = false, updatable = false)
    private LocalDateTime drawTimestamp;
    
    /**
     * Cost type (1=gold, 2=diamond, 3=ticket)
     */
    @Column(name = "cost_type", nullable = false)
    private Integer costType;
    
    /**
     * Cost amount
     */
    @Column(name = "cost_amount", nullable = false)
    private Long costAmount;
}
