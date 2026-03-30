package com.SouthMillion.moderation_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reports")
public class Report {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reporter_id", nullable = false, length = 64)
    private String reporterId;

    @Column(name = "reported_user_id", nullable = false, length = 64)
    private String reportedUserId;
    
    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // CHAT_SPAM, CHAT_ABUSE, CHEATING, INAPPROPRIATE_NAME
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence; // JSON data
    
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, REVIEWING, RESOLVED, REJECTED
    
    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;
    
    @Column(name = "handled_by", length = 64)
    private String handledBy; // GM ID
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
}
