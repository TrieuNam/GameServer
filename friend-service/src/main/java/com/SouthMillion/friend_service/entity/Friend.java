package com.SouthMillion.friend_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Friend Relationship Entity
 * 
 * Bidirectional friendship (both players have each other as friends)
 */
@Entity
@Table(name = "friend", indexes = {
    @Index(name = "idx_friend_role1", columnList = "roleId1"),
    @Index(name = "idx_friend_role2", columnList = "roleId2")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_friend_pair", columnNames = {"roleId1", "roleId2"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Player 1 role ID (always the smaller ID for consistency)
     */
    @Column(nullable = false)
    private Long roleId1;

    /**
     * Player 1 name
     */
    @Column(nullable = false, length = 50)
    private String roleName1;

    /**
     * Player 2 role ID
     */
    @Column(nullable = false)
    private Long roleId2;

    /**
     * Player 2 name
     */
    @Column(nullable = false, length = 50)
    private String roleName2;

    /**
     * Friendship level (1-5, increases with interaction)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer friendshipLevel = 1;

    /**
     * Friendship points (for leveling up)
     */
    @Column(nullable = false)
    @Builder.Default
    private Long friendshipPoints = 0L;

    /**
     * Last interaction time
     */
    private LocalDateTime lastInteractionTime;

    /**
     * Friend since
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Add friendship points
     */
    public void addFriendshipPoints(long points) {
        this.friendshipPoints += points;
        this.lastInteractionTime = LocalDateTime.now();
        
        // Level up logic (every 1000 points = 1 level)
        while (friendshipLevel < 5 && friendshipPoints >= getPointsForNextLevel()) {
            friendshipPoints -= getPointsForNextLevel();
            friendshipLevel++;
        }
    }

    /**
     * Get points required for next level
     */
    public long getPointsForNextLevel() {
        if (friendshipLevel >= 5) return 0;
        return friendshipLevel * 1000L;
    }

    /**
     * Check if roleId is in this friendship
     */
    public boolean involves(Long roleId) {
        return roleId1.equals(roleId) || roleId2.equals(roleId);
    }

    /**
     * Get the other player's ID
     */
    public Long getOtherRoleId(Long roleId) {
        return roleId1.equals(roleId) ? roleId2 : roleId1;
    }

    /**
     * Get the other player's name
     */
    public String getOtherRoleName(Long roleId) {
        return roleId1.equals(roleId) ? roleName2 : roleName1;
    }
}
