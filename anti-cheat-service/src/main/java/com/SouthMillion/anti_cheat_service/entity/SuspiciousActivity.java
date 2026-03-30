package com.SouthMillion.anti_cheat_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspicious_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;
    
    @Column(name = "suspicion_score", nullable = false)
    private Integer suspicionScore; // 0-100
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON data
    
    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;
    
    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
