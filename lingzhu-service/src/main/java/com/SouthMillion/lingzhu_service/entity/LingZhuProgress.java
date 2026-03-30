package com.SouthMillion.lingzhu_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Tracks lord dungeon (领主副本) progress per role and stage.
 * stage maps to the dungeon type/difficulty level.
 */
@Entity
@Table(name = "lingzhu_progress",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_stage", columnNames = {"role_id", "stage"}),
    indexes = @Index(name = "idx_role_id", columnList = "role_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LingZhuProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** Dungeon stage/type identifier */
    @Column(name = "stage", nullable = false)
    private Integer stage;

    /** Highest level passed in this stage */
    @Column(name = "pass_level", nullable = false)
    private Integer passLevel;

    /** Number of sweep (auto-clear) uses remaining today */
    @Column(name = "sweep_count", nullable = false)
    private Integer sweepCount;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
