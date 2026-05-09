package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Type 31: 新服比拼全局 (New Server Competition Global)
 * Server-wide global data for new server competition (not per-role).
 * Stores end times for multiple competition phases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "new_server_global")
@Component
public class NewServerGlobal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false, unique = true)
    private Integer serverId;

    @Column(name = "end_time_json", nullable = false, columnDefinition = "TEXT")
    private String endTimeJson; // JSON array of phase end timestamps

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @Value("${gameplay.reward.amount}")
    private int rewardAmount;

    @Value("${gameplay.unlock.threshold}")
    private int unlockThreshold;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}