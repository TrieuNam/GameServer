package com.SouthMillion.anti_cheat_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cheat_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheatReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "cheat_type", nullable = false, length = 50)
    private String cheatType; // SPEED_HACK, TELEPORT, DAMAGE_HACK, RESOURCE_HACK, MEMORY_MODIFICATION, BOT
    
    @Column(name = "severity", nullable = false)
    private Integer severity; // 1=Low, 2=Medium, 3=High, 4=Critical
    
    @Column(name = "confidence", nullable = false)
    private Double confidence; // 0.0 - 1.0
    
    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence; // JSON data
    
    @Column(name = "status", nullable = false, length = 20)
    private String status; // DETECTED, REVIEWING, CONFIRMED, FALSE_POSITIVE, BANNED
    
    @Column(name = "action_taken", length = 50)
    private String actionTaken; // WARNING, TEMPORARY_BAN, PERMANENT_BAN, NONE
    
    @Column(name = "reviewed_by")
    private Long reviewedBy; // GM ID
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
