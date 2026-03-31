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
 * Blocked Player Entity
 * 
 * Represents a blocked relationship (one-way block)
 */
@Entity
@Table(name = "blocked_player", indexes = {
    @Index(name = "idx_blocked_blocker", columnList = "blocker_id"),
    @Index(name = "idx_blocked_target", columnList = "blocked_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_block_pair", columnNames = {"blocker_id", "blocked_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Blocker role ID (who blocked)
     */
    @Column(nullable = false)
    private Long blockerId;

    /**
     * Blocked player role ID
     */
    @Column(nullable = false)
    private Long blockedId;

    /**
     * Blocked player name
     */
    @Column(nullable = false, length = 50)
    private String blockedName;

    /**
     * Block reason
     */
    @Column(length = 200)
    private String reason;

    /**
     * Block time
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime blockedAt;
}
