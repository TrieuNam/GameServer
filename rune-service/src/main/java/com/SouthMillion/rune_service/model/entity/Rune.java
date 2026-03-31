package com.SouthMillion.rune_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Rune Entity
 * Rune item that can be equipped on gear for power boost
 * C++ Source: gameworld/other/rune/
 */
@Entity
@Table(name = "rune", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_rune", columnList = "user_id,rune_index"),
    @Index(name = "idx_equipped", columnList = "user_id,is_equipped")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rune {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Unique rune index for this user
     */
    @Column(name = "rune_index", nullable = false)
    private Integer runeIndex;
    
    /**
     * Rune config ID (rune type)
     */
    @Column(name = "rune_id", nullable = false)
    private Integer runeId;
    
    /**
     * Rune level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Rune quality/grade (1-5: white, green, blue, purple, orange)
     */
    @Column(name = "quality", nullable = false)
    private Integer quality;
    
    /**
     * Rune star rating
     */
    @Column(name = "star", nullable = false)
    private Integer star;
    
    /**
     * Is rune equipped on gear
     */
    @Column(name = "is_equipped", nullable = false)
    private Boolean isEquipped;
    
    /**
     * Equipment slot where rune is equipped (null if not equipped)
     */
    @Column(name = "equip_slot")
    private Integer equipSlot;
    
    /**
     * Main attribute type
     */
    @Column(name = "main_attr_type", nullable = false)
    private Integer mainAttrType;
    
    /**
     * Main attribute value
     */
    @Column(name = "main_attr_value", nullable = false)
    private Long mainAttrValue;
    
    /**
     * Sub attribute 1 type
     */
    @Column(name = "sub_attr1_type")
    private Integer subAttr1Type;
    
    /**
     * Sub attribute 1 value
     */
    @Column(name = "sub_attr1_value")
    private Long subAttr1Value;
    
    /**
     * Sub attribute 2 type
     */
    @Column(name = "sub_attr2_type")
    private Integer subAttr2Type;
    
    /**
     * Sub attribute 2 value
     */
    @Column(name = "sub_attr2_value")
    private Long subAttr2Value;
    
    /**
     * Sub attribute 3 type
     */
    @Column(name = "sub_attr3_type")
    private Integer subAttr3Type;
    
    /**
     * Sub attribute 3 value
     */
    @Column(name = "sub_attr3_value")
    private Long subAttr3Value;
    
    /**
     * Refinement level (强化等级)
     */
    @Column(name = "refinement_level", nullable = false)
    private Integer refinementLevel;
    
    /**
     * Experience points
     */
    @Column(name = "exp", nullable = false)
    private Long exp;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
