package com.SouthMillion.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "admin_users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminId;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AdminRole role = AdminRole.GAME_MASTER;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(columnDefinition = "JSON")
    private String permissions; // JSON array of permission strings
    
    @CreationTimestamp
    private Instant createTime;
    
    @UpdateTimestamp
    private Instant updateTime;
    
    private Instant lastLoginTime;
    
    public enum AdminRole {
        SUPER_ADMIN,    // Full access
        GAME_MASTER,    // Player management
        MODERATOR,      // Limited access
        VIEWER          // Read-only
    }
}
