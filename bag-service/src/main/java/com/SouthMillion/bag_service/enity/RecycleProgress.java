package com.SouthMillion.bag_service.enity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks per-role item recycle progression (物品回收).
 * One row per role.
 *
 * level — current recycle level
 * exp   — accumulated exp toward next level (threshold: 100 exp per level)
 */
@Entity
@Table(name = "recycle_progress",
    uniqueConstraints = @UniqueConstraint(name = "uq_recycle_role", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecycleProgress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "exp", nullable = false)
    private Long exp;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
