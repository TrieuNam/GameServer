package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 33: 无限战令 (Infinite War Order / Battle Pass)
 * Complex progression system with level, exp, daily/weekly tasks, and dual reward tracks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "war_order")
public class WarOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "open_level", nullable = false)
    private Integer openLevel; // Level requirement to access

    @Column(name = "is_buy", nullable = false)
    private Integer isBuy; // 0=free track only, 1=premium track purchased

    @Column(name = "time_seq_timestamp", nullable = false)
    private Integer timeSeqTimestamp; // Season/period timestamp

    @Column(name = "level", nullable = false)
    private Integer level; // Current battle pass level

    @Column(name = "exp", nullable = false)
    private Integer exp; // Current experience points

    @Column(name = "common_fetch_flag", nullable = false)
    private Long commonFetchFlag; // Bitmask for free track rewards

    @Column(name = "senior_fetch_flag", nullable = false)
    private Long seniorFetchFlag; // Bitmask for premium track rewards

    @Column(name = "day_task_flag", nullable = false)
    private Integer dayTaskFlag; // Bitmask for daily task completion

    @Column(name = "week_task_flag", nullable = false)
    private Integer weekTaskFlag; // Bitmask for weekly task completion

    @Column(name = "day_task_num_json", nullable = false, columnDefinition = "TEXT")
    private String dayTaskNumJson; // JSON array of daily task progress

    @Column(name = "week_task_num_json", nullable = false, columnDefinition = "TEXT")
    private String weekTaskNumJson; // JSON array of weekly task progress

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
