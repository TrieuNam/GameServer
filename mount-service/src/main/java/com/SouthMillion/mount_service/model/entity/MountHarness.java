package com.SouthMillion.mount_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Mount Harness Entity
 * Represents harness/equipment items in player's bag (not equipped)
 * Similar to pet gems - stored in bag until equipped on mount
 * 
 * C++ Source: gameworld/other/mount/
 * DB Table: mount_harness_bag
 */
@Entity
@Table(name = "mount_harness", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_harness", columnList = "user_id,harness_index")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MountHarness {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User ID (owner)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Harness bag index (0-based)
     */
    @Column(name = "harness_index", nullable = false)
    private Integer harnessIndex;
    
    /**
     * Harness item ID from config
     */
    @Column(name = "item_id", nullable = false)
    private Integer itemId;
    
    /**
     * Harness level
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Harness grade/quality
     */
    @Column(name = "grade", nullable = false)
    private Integer grade;
    
    /**
     * Entry 1 type (attribute type)
     */
    @Column(name = "entry1_type")
    private Integer entry1Type;
    
    /**
     * Entry 1 value (attribute value)
     */
    @Column(name = "entry1_value")
    private Long entry1Value;
    
    /**
     * Entry 2 type
     */
    @Column(name = "entry2_type")
    private Integer entry2Type;
    
    /**
     * Entry 2 value
     */
    @Column(name = "entry2_value")
    private Long entry2Value;
    
    /**
     * Entry 3 type
     */
    @Column(name = "entry3_type")
    private Integer entry3Type;
    
    /**
     * Entry 3 value
     */
    @Column(name = "entry3_value")
    private Long entry3Value;
    
    /**
     * Entry 4 type
     */
    @Column(name = "entry4_type")
    private Integer entry4Type;
    
    /**
     * Entry 4 value
     */
    @Column(name = "entry4_value")
    private Long entry4Value;

    // ─── Slots 5–8: added to match PB_HarnessData.attr_type[8] (proto repeated[8]) ─
    @Column(name = "entry5_type")  private Integer entry5Type;
    @Column(name = "entry5_value") private Long    entry5Value;
    @Column(name = "entry6_type")  private Integer entry6Type;
    @Column(name = "entry6_value") private Long    entry6Value;
    @Column(name = "entry7_type")  private Integer entry7Type;
    @Column(name = "entry7_value") private Long    entry7Value;
    @Column(name = "entry8_type")  private Integer entry8Type;
    @Column(name = "entry8_value") private Long    entry8Value;

    /**
     * Lock flags for entries (bitmask: bits 0-7 for entry 1-8)
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
