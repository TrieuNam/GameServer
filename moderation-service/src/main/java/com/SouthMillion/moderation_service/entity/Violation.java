package com.SouthMillion.moderation_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "violations")
public class Violation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "violation_type", nullable = false, length = 50)
    private String violationType; // BAD_WORD, SPAM, INAPPROPRIATE_CONTENT
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "severity", nullable = false)
    private Integer severity = 1; // 1=Minor, 2=Moderate, 3=Severe
    
    @Column(name = "action_taken", length = 50)
    private String actionTaken; // WARNING, MUTE, BAN
    
    @Column(name = "duration_hours")
    private Integer durationHours;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
