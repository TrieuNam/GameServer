package com.SouthMillion.arenaservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "arena_players", indexes = {
    @Index(name = "idx_rating", columnList = "rating DESC"),
    @Index(name = "idx_current_rank", columnList = "currentRank")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaPlayer {
    
    @Id
    @Column(length = 50)
    private String playerId;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer rating = 1000;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer wins = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer losses = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer currentRank = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer consecutiveWins = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalBattles = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer season = 1;
    
    @CreationTimestamp
    private Instant createTime;
    
    @UpdateTimestamp
    private Instant updateTime;
    
    private Instant lastBattleTime;

    /** Number of challenges used today (resets daily) */
    @Column(nullable = false)
    @Builder.Default
    private Integer challengesUsedToday = 0;

    /** Date of last daily challenge reset */
    private LocalDate lastResetDate;

    public Integer getWinRate() {
        if (totalBattles == 0) return 0;
        return (wins * 100) / totalBattles;
    }
}
