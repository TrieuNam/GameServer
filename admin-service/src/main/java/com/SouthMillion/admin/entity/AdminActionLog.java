package com.SouthMillion.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "admin_action_logs", indexes = {
    @Index(name = "idx_admin_id", columnList = "adminId"),
    @Index(name = "idx_timestamp", columnList = "timestamp DESC"),
    @Index(name = "idx_target_player", columnList = "targetPlayerId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;
    
    @Column(nullable = false)
    private Long adminId;
    
    @Column(nullable = false, length = 100)
    private String action;
    
    @Column(length = 50)
    private String targetPlayerId;
    
    @Column(columnDefinition = "JSON")
    private String details; // JSON object with action details
    
    @Column(length = 50)
    private String ipAddress;
    
    @CreationTimestamp
    @Column(nullable = false)
    private Instant timestamp;
}
