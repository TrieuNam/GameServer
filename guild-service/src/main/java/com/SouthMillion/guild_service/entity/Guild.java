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
 * Guild Entity
 * 
 * Main entity for guild system
 * Max members: 50
 * Max level: 10
 * Technology branches: 5 (ATK/DEF/HP/CRT/SPD)
 */
@Entity
@Table(name = "guild", indexes = {
    @Index(name = "idx_guild_name", columnList = "name"),
    @Index(name = "idx_guild_level", columnList = "level"),
    @Index(name = "idx_guild_leader", columnList = "leaderId")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Guild name (unique, 2-20 characters)
     */
    @Column(nullable = false, unique = true, length = 20)
    private String name;

    /**
     * Guild leader role ID
     */
    @Column(nullable = false)
    private Long leaderId;

    /**
     * Guild level (1-10)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    /**
     * Guild experience points
     */
    @Column(nullable = false)
    @Builder.Default
    private Long exp = 0L;

    /**
     * Current member count
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer memberCount = 1;

    /**
     * Maximum member capacity (based on level)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxMembers = 50;

    /**
     * Guild notice/announcement
     */
    @Column(length = 500)
    private String notice;

    /**
     * Technology level - Attack (1-10)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer techAttack = 1;

    /**
     * Technology level - Defense (1-10)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer techDefense = 1;

    /**
     * Technology level - HP (1-10)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer techHp = 1;

    /**
     * Technology level - Critical (1-10)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer techCrit = 1;

    /**
     * Technology level - Speed (1-10)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer techSpeed = 1;

    /**
     * Guild funds (gold)
     */
    @Column(nullable = false)
    @Builder.Default
    private Long funds = 0L;

    /**
     * Daily donation limit reset time
     */
    private LocalDateTime donationResetTime;

    /**
     * Guild creation time
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Guild last update time
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Guild disbandment time (null if active)
     */
    private LocalDateTime disbandedAt;

    /**
     * Is guild active?
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Get experience required for next level
     */
    public long getExpForNextLevel() {
        if (level >= 10) return 0L;
        return (long) (level * 10000L);
    }

    /**
     * Add experience and check for level up
     * @return true if leveled up
     */
    public boolean addExp(long amount) {
        if (level >= 10) return false;
        
        exp += amount;
        long required = getExpForNextLevel();
        
        if (exp >= required) {
            level++;
            exp -= required;
            return true;
        }
        return false;
    }

    /**
     * Check if can upgrade technology
     */
    public boolean canUpgradeTech(String techType) {
        int currentLevel = getTechLevel(techType);
        return currentLevel < 10 && funds >= getTechUpgradeCost(techType);
    }

    /**
     * Get technology level by type
     */
    public int getTechLevel(String techType) {
        return switch (techType.toUpperCase()) {
            case "ATTACK", "ATK" -> techAttack;
            case "DEFENSE", "DEF" -> techDefense;
            case "HP", "HEALTH" -> techHp;
            case "CRITICAL", "CRT", "CRIT" -> techCrit;
            case "SPEED", "SPD" -> techSpeed;
            default -> 0;
        };
    }

    /**
     * Upgrade technology
     */
    public boolean upgradeTech(String techType) {
        if (!canUpgradeTech(techType)) return false;
        
        long cost = getTechUpgradeCost(techType);
        funds -= cost;
        
        switch (techType.toUpperCase()) {
            case "ATTACK", "ATK" -> techAttack++;
            case "DEFENSE", "DEF" -> techDefense++;
            case "HP", "HEALTH" -> techHp++;
            case "CRITICAL", "CRT", "CRIT" -> techCrit++;
            case "SPEED", "SPD" -> techSpeed++;
            default -> { return false; }
        }
        
        return true;
    }

    /**
     * Get technology upgrade cost
     */
    public long getTechUpgradeCost(String techType) {
        int currentLevel = getTechLevel(techType);
        return (long) ((currentLevel + 1) * 5000L);
    }

    /**
     * Check if member limit reached
     */
    public boolean isFull() {
        return memberCount >= maxMembers;
    }

    /**
     * Add funds (from donations)
     */
    public void addFunds(long amount) {
        this.funds += amount;
    }
}
