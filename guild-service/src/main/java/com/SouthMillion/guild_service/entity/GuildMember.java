package com.SouthMillion.guild_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Guild Member Entity
 * 
 * Represents a member of a guild
 * Ranks: 1=Member, 2=Officer, 3=Leader
 */
@Entity
@Table(name = "guild_member", indexes = {
    @Index(name = "idx_member_guild", columnList = "guildId"),
    @Index(name = "idx_member_role", columnList = "roleId"),
    @Index(name = "idx_member_rank", columnList = "rank")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_guild_role", columnNames = {"guildId", "roleId"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuildMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Guild ID
     */
    @Column(nullable = false)
    private Long guildId;

    /**
     * Role ID (player character ID)
     */
    @Column(nullable = false)
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
    private Integer roleLevel;

    /**
     * Player power/combat rating
     */
    @Column(nullable = false)
    @Builder.Default
    private Long power = 0L;

    /**
     * Member rank in guild
     * 1 = Member
     * 2 = Officer
     * 3 = Leader
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer rank = 1;

    /**
     * Total contribution to guild
     */
    @Column(nullable = false)
    @Builder.Default
    private Long contribution = 0L;

    /**
     * Daily donation count
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer dailyDonationCount = 0;

    /**
     * Last donation time
     */
    private LocalDateTime lastDonationTime;

    /**
     * Last online time
     */
    private LocalDateTime lastOnlineTime;

    /**
     * Is member online?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean online = false;

    /**
     * Join time
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * Last update time
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if is leader
     */
    public boolean isLeader() {
        return rank == 3;
    }

    /**
     * Check if is officer or leader
     */
    public boolean isOfficerOrAbove() {
        return rank >= 2;
    }

    /**
     * Check if can donate today
     */
    public boolean canDonateToday() {
        return dailyDonationCount < 3; // Max 3 donations per day
    }

    /**
     * Add contribution
     */
    public void addContribution(long amount) {
        this.contribution += amount;
    }

    /**
     * Donate (increment daily count)
     */
    public void donate() {
        this.dailyDonationCount++;
        this.lastDonationTime = LocalDateTime.now();
    }

    /**
     * Reset daily donation count
     */
    public void resetDailyDonation() {
        this.dailyDonationCount = 0;
    }

    /**
     * Promote to next rank
     * @return true if promoted
     */
    public boolean promote() {
        if (rank >= 3) return false;
        rank++;
        return true;
    }

    /**
     * Demote to previous rank
     * @return true if demoted
     */
    public boolean demote() {
        if (rank <= 1) return false;
        rank--;
        return true;
    }

    /**
     * Update online status
     */
    public void updateOnlineStatus(boolean isOnline) {
        this.online = isOnline;
        if (isOnline) {
            this.lastOnlineTime = LocalDateTime.now();
        }
    }
}
