package com.SouthMillion.guild_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Guild Application Entity
 * 
 * Represents a player's application to join a guild
 */
@Entity
@Table(name = "guild_application", indexes = {
    @Index(name = "idx_app_guild", columnList = "guildId"),
    @Index(name = "idx_app_role", columnList = "roleId"),
    @Index(name = "idx_app_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_app_guild_role", columnNames = {"guildId", "roleId"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuildApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Guild ID
     */
    @Column(nullable = false)
    private Long guildId;

    /**
     * Applicant role ID
     */
    @Column(nullable = false)
    private Long roleId;

    /**
     * Applicant name
     */
    @Column(nullable = false, length = 50)
    private String roleName;

    /**
     * Applicant level
     */
    @Column(nullable = false)
    private Integer roleLevel;

    /**
     * Applicant power
     */
    @Column(nullable = false)
    @Builder.Default
    private Long power = 0L;

    /**
     * Application message
     */
    @Column(length = 200)
    private String message;

    /**
     * Application status
     * 0 = Pending
     * 1 = Approved
     * 2 = Rejected
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0;

    /**
     * Processor role ID (who approved/rejected)
     */
    private String processorId;

    /**
     * Process time
     */
    private LocalDateTime processedAt;

    /**
     * Application time
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    /**
     * Check if pending
     */
    public boolean isPending() {
        return status == 0;
    }

    /**
     * Approve application
     */
    public void approve(String processorId) {
        this.status = 1;
        this.processorId = processorId;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Reject application
     */
    public void reject(String processorId) {
        this.status = 2;
        this.processorId = processorId;
        this.processedAt = LocalDateTime.now();
    }
}
