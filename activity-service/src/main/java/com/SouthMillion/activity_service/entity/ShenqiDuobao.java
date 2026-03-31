package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 37: 神器夺宝 (ShenQi Duobao / Artifact Treasure Hunt)
 * Activity with task progression and purchasable gift packs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shenqi_duobao")
public class ShenqiDuobao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "role_level", nullable = false)
    private Integer roleLevel; // Player level snapshot

    @Column(name = "tasks_json", nullable = false, columnDefinition = "TEXT")
    private String tasksJson; // JSON array of {taskSeq, progress, hasFetched}

    @Column(name = "gifts_json", nullable = false, columnDefinition = "TEXT")
    private String giftsJson; // JSON array of {giftSeq, buyTimes}

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
