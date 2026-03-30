package com.SouthMillion.angel_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Angel Entity
 * Represents player's angel/wing companion
 * 
 * C++ Source: gameworld/other/angel/
 * Config: angel.json
 * DB Table: angel_data
 */
@Entity
@Table(name = "angel", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_angel", columnList = "user_id,angel_index")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Angel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User ID (owner)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Angel index/slot (0-based, multiple angels)
     */
    @Column(name = "angel_index", nullable = false)
    private Integer angelIndex;
    
    /**
     * Angel config ID from angel.json
     */
    @Column(name = "angel_id", nullable = false)
    private Integer angelId;
    
    /**
     * Angel level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Angel grade/quality
     */
    @Column(name = "grade", nullable = false)
    private Integer grade;
    
    /**
     * Current experience
     */
    @Column(name = "exp", nullable = false)
    private Long exp;
    
    /**
     * Is angel activated/unlocked
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * Is currently equipped/displayed
     */
    @Column(name = "is_equipped", nullable = false)
    private Boolean isEquipped;
    
    /**
     * Star level (upgrade tier)
     */
    @Column(name = "star_level", nullable = false)
    private Integer starLevel;
    
    /**
     * Skill 1 ID
     */
    @Column(name = "skill1_id")
    private Integer skill1Id;
    
    /**
     * Skill 1 level
     */
    @Column(name = "skill1_level", nullable = false)
    private Integer skill1Level;
    
    /**
     * Skill 2 ID
     */
    @Column(name = "skill2_id")
    private Integer skill2Id;
    
    /**
     * Skill 2 level
     */
    @Column(name = "skill2_level", nullable = false)
    private Integer skill2Level;
    
    /**
     * Skill 3 ID
     */
    @Column(name = "skill3_id")
    private Integer skill3Id;
    
    /**
     * Skill 3 level
     */
    @Column(name = "skill3_level", nullable = false)
    private Integer skill3Level;
    
    /**
     * Skill 4 ID (passive)
     */
    @Column(name = "skill4_id")
    private Integer skill4Id;
    
    /**
     * Skill 4 level
     */
    @Column(name = "skill4_level", nullable = false)
    private Integer skill4Level;
    
    /**
     * Current appearance/skin ID
     */
    @Column(name = "appearance_id")
    private Integer appearanceId;
    
    /**
     * Appearance level/quality
     */
    @Column(name = "appearance_level", nullable = false)
    private Integer appearanceLevel = 0;
    
    /**
     * Evolution stage
     */
    @Column(name = "evolution_stage", nullable = false)
    private Integer evolutionStage;
    
    /**
     * Blessing/buff points
     */
    @Column(name = "blessing_points", nullable = false)
    private Long blessingPoints;

    /**
     * Angel custom name (set by player via rename)
     */
    @Column(name = "name", length = 32)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
