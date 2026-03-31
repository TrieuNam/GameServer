package com.SouthMillion.analytics_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "player_kpi", indexes = {
    @Index(name = "idx_player_date", columnList = "playerId,date")
})
@EntityListeners(AuditingEntityListener.class)
public class PlayerKpi {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long playerId;
    
    @Column(nullable = false)
    private LocalDateTime date;
    
    // Engagement Metrics
    @Column(nullable = false)
    private Integer loginCount = 0;
    
    @Column(nullable = false)
    private Integer sessionDuration = 0; // seconds
    
    // Economy Metrics
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEarned = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private Integer purchaseCount = 0;
    
    // Combat Metrics
    @Column(nullable = false)
    private Integer battlesPlayed = 0;
    
    @Column(nullable = false)
    private Integer battlesWon = 0;
    
    @Column(nullable = false)
    private Integer pvpMatches = 0;
    
    // Social Metrics
    @Column(nullable = false)
    private Integer messagesent = 0;
    
    @Column(nullable = false)
    private Integer friendRequests = 0;
    
    // Progression Metrics
    @Column(nullable = false)
    private Integer tasksCompleted = 0;
    
    @Column(nullable = false)
    private Integer levelsGained = 0;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
