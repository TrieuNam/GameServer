package com.SouthMillion.friend_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Online Status Entity
 * 
 * Tracks player online/offline status
 */
@Entity
@Table(name = "online_status", indexes = {
    @Index(name = "idx_status_role", columnList = "roleId"),
    @Index(name = "idx_status_online", columnList = "online")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Player role ID
     */
    @Column(nullable = false, unique = true)
    private Long roleId;

    /**
     * Player name
     */
    @Column(nullable = false, length = 50)
    private String roleName;

    /**
     * Player level
     */
    @Column(nullable = false)
    private Integer level;

    /**
     * Is online?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean online = false;

    /**
     * Last login time
     */
    private LocalDateTime lastLoginTime;

    /**
     * Last logout time
     */
    private LocalDateTime lastLogoutTime;

    /**
     * Last update time
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Set online
     */
    public void setOnline() {
        this.online = true;
        this.lastLoginTime = LocalDateTime.now();
    }

    /**
     * Set offline
     */
    public void setOffline() {
        this.online = false;
        this.lastLogoutTime = LocalDateTime.now();
    }
}
