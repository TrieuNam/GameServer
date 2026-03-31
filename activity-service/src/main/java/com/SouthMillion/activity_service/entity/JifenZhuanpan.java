package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 40: 积分转盘 (Jifen Zhuanpan / Points Wheel)
 * Spin wheel system with points, big prize pity counter, and reward tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jifen_zhuanpan")
public class JifenZhuanpan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "role_level", nullable = false)
    private Integer roleLevel; // Player level

    @Column(name = "reward_group", nullable = false)
    private Integer rewardGroup; // Reward pool group ID

    @Column(name = "times_to_big_prize", nullable = false)
    private Integer timesToBigPrize; // Spins until guaranteed big prize (pity counter)

    @Column(name = "jifen", nullable = false)
    private Integer jifen; // Accumulated points

    @Column(name = "reward_seqs_json", nullable = false, columnDefinition = "TEXT")
    private String rewardSeqsJson; // JSON array of obtained reward sequence IDs

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
