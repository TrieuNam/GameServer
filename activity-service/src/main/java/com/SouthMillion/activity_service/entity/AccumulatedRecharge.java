package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Accumulated Recharge (累充) for RandActivity type 13.
 * Milestone-based rewards when cumulative recharge reaches certain amounts.
 */
@Entity
@Table(name = "accumulated_recharge")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccumulatedRecharge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Bitmask of claimed milestone rewards (领取标记) */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
