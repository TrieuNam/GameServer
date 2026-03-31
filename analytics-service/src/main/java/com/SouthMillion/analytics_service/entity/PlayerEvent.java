package com.SouthMillion.analytics_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "player_events", indexes = {
    @Index(name = "idx_player_event", columnList = "playerId,eventType"),
    @Index(name = "idx_event_time", columnList = "eventTime")
})
@EntityListeners(AuditingEntityListener.class)
public class PlayerEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long playerId;
    
    @Column(nullable = false, length = 50)
    private String eventType; // login, logout, level_up, purchase, battle, etc.
    
    @Column(nullable = false, length = 50)
    private String eventCategory; // gameplay, economy, social, combat
    
    @Column(columnDefinition = "TEXT")
    private String eventData; // JSON payload
    
    @Column(nullable = false)
    private LocalDateTime eventTime;
    
    @Column(length = 20)
    private String serverName;
    
    @Column(length = 50)
    private String sessionId;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
