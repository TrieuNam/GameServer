package com.SouthMillion.notification_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_player_status", columnList = "playerId,status"),
    @Index(name = "idx_type", columnList = "type")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long playerId;
    
    @Column(nullable = false, length = 50)
    private String type; // PUSH, EMAIL, SMS, IN_GAME
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(columnDefinition = "TEXT")
    private String data; // JSON payload
    
    @Column(nullable = false, length = 20)
    private String status; // PENDING, SENT, FAILED, READ
    
    @Column(length = 500)
    private String errorMessage;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime readAt;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
