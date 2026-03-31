package com.SouthMillion.artifact_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Artifact Entity
 * Divine weapon/artifact system (ShenQi 神器)
 * 
 * C++ Source: gameworld/other/shenqi/
 * Config: shenqi.json
 * DB Table: shenqi_data
 */
@Entity
@Table(name = "artifact", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_artifact", columnList = "user_id,artifact_index")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Artifact {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User ID (owner)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Artifact index/slot
     */
    @Column(name = "artifact_index", nullable = false)
    private Integer artifactIndex;
    
    /**
     * Artifact config ID from shenqi.json
     */
    @Column(name = "artifact_id", nullable = false)
    private Integer artifactId;
    
    /**
     * Artifact level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Artifact grade/rank
     */
    @Column(name = "grade", nullable = false)
    private Integer grade;
    
    /**
     * Current experience
     */
    @Column(name = "exp", nullable = false)
    private Long exp;
    
    /**
     * Is artifact activated/unlocked
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    /**
     * Is currently equipped
     */
    @Column(name = "is_equipped", nullable = false)
    private Boolean isEquipped;
    
    /**
     * Refinement level (精炼等级)
     */
    @Column(name = "refinement_level", nullable = false)
    private Integer refinementLevel;
    
    /**
     * Awakening stage (觉醒阶段)
     */
    @Column(name = "awakening_stage", nullable = false)
    private Integer awakeningStage;
    
    /**
     * Soul power (魂力值)
     */
    @Column(name = "soul_power", nullable = false)
    private Long soulPower;
    
    /**
     * Divine essence (神性精华)
     */
    @Column(name = "divine_essence", nullable = false)
    private Long divineEssence;
    
    /**
     * Attribute 1 type
     */
    @Column(name = "attr1_type")
    private Integer attr1Type;
    
    /**
     * Attribute 1 value
     */
    @Column(name = "attr1_value")
    private Long attr1Value;
    
    /**
     * Attribute 2 type
     */
    @Column(name = "attr2_type")
    private Integer attr2Type;
    
    /**
     * Attribute 2 value
     */
    @Column(name = "attr2_value")
    private Long attr2Value;
    
    /**
     * Attribute 3 type
     */
    @Column(name = "attr3_type")
    private Integer attr3Type;
    
    /**
     * Attribute 3 value
     */
    @Column(name = "attr3_value")
    private Long attr3Value;
    
    /**
     * Attribute 4 type (special/hidden)
     */
    @Column(name = "attr4_type")
    private Integer attr4Type;
    
    /**
     * Attribute 4 value
     */
    @Column(name = "attr4_value")
    private Long attr4Value;
    
    /**
     * Blessing tier (祝福层级)
     */
    @Column(name = "blessing_tier", nullable = false)
    private Integer blessingTier;
    
    /**
     * Skill 1 level (技能1等级)
     */
    @Column(name = "skill1_level", nullable = false)
    private Integer skill1Level = 0;
    
    /**
     * Skill 2 level (技能2等级)
     */
    @Column(name = "skill2_level", nullable = false)
    private Integer skill2Level = 0;
    
    /**
     * Skill 3 level (技能3等级)
     */
    @Column(name = "skill3_level", nullable = false)
    private Integer skill3Level = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
