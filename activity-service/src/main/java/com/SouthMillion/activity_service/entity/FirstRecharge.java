package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * First Recharge (首充) for RandActivity type 12.
 * Tracks if player has made first recharge and claimed the reward.
 */
@Entity
@Table(name = "first_recharge")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FirstRecharge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** 0=not recharged, 1=recharged (首充标记) */
    @Column(name = "first_chong_mark", nullable = false)
    private Integer firstChongMark;

    /** 0=not claimed, 1=claimed (领取标记) */
    @Column(name = "fetch_mark", nullable = false)
    private Integer fetchMark;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
