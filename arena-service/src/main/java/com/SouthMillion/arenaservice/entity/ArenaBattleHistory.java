package com.SouthMillion.arenaservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "arena_battle_history", indexes = {
    @Index(name = "idx_player1", columnList = "player1_id"),
    @Index(name = "idx_player2", columnList = "player2_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaBattleHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_id")
    private Long battleId;
    
    @Column(name = "player1_id", nullable = false, length = 50)
    private String player1Id;
    
    @Column(name = "player2_id", nullable = false, length = 50)
    private String player2Id;
    
    @Column(name = "winner_id", nullable = false, length = 50)
    private String winnerId;
    
    @Column(name = "rating_change", nullable = false)
    private Integer ratingChange;
    
    @Column(name = "player1_rating_before", nullable = false)
    private Integer player1RatingBefore;
    
    @Column(name = "player2_rating_before", nullable = false)
    private Integer player2RatingBefore;
    
    @Column(name = "player1_rating_after", nullable = false)
    private Integer player1RatingAfter;
    
    @Column(name = "player2_rating_after", nullable = false)
    private Integer player2RatingAfter;
    
    @Column(name = "battle_duration", nullable = false)
    private Integer battleDuration; // seconds
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
