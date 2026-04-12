package com.SouthMillion.role_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Limit-core (限界突破) level record per player per type.
 *
 * <p>limit_type values (matches client CoreCrisisType):
 * <ul>
 *   <li>1 = Mount (坐骑)     chip itemId 40500</li>
 *   <li>2 = Angel (法阵)     chip itemId 40501</li>
 *   <li>3 = Gem (宝石)       chip itemId 40502</li>
 *   <li>4 = StarMap (星图)   chip itemId 40503</li>
 *   <li>5 = Inscription(铭文) chip itemId 40504</li>
 *   <li>6 = ShenQi (神器)    chip itemId 40505</li>
 * </ul>
 */
@Entity
@Table(
        name = "player_limit_core",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_limit_type",
                columnNames = {"role_id", "limit_type"}
        ),
        indexes = @Index(name = "idx_plc_role_id", columnList = "role_id")
)
@Getter
@Setter
public class PlayerLimitCore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "limit_type", nullable = false)
    private Integer limitType;

    @Column(name = "level", nullable = false)
    private Integer level = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = Instant.now();
    }
}
