package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 36: 新服比拼排行榜 (New Server Competition Ranking)
 * Player's personal ranking data in various new server competition categories.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "new_server_ranking")
public class NewServerRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "ranking_type", nullable = false)
    private Integer rankingType; // Category type (combat power, level, etc.)

    @Column(name = "my_rank", nullable = false)
    private Integer myRank; // Current rank position

    @Column(name = "my_rank_value", nullable = false)
    private Long myRankValue; // Score/value for current ranking

    @Column(name = "my_best_rank", nullable = false)
    private Integer myBestRank; // Historical best rank achieved

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
