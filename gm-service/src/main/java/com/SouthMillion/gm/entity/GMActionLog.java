package com.SouthMillion.gm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "gm_action_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GMActionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long gmId;
    
    @Column(nullable = false, length = 50)
    private String gmUsername;
    
    @Column(nullable = false, length = 100)
    private String action;
    
    @Column(length = 50)
    private String targetPlayerId;
    
    private Long targetUserId;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(length = 255)
    private String reason;
    
    @Column(length = 50)
    private String ipAddress;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Column(length = 20)
    private String status; // SUCCESS, FAILED
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
