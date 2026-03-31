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
 * Friend Request Entity
 * 
 * Represents a pending friend request from sender to receiver
 */
@Entity
@Table(name = "friend_request", indexes = {
    @Index(name = "idx_request_sender", columnList = "senderId"),
    @Index(name = "idx_request_receiver", columnList = "receiverId"),
    @Index(name = "idx_request_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_request_pair", columnNames = {"senderId", "receiverId"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sender role ID
     */
    @Column(nullable = false)
    private Long senderId;

    /**
     * Sender name
     */
    @Column(nullable = false, length = 50)
    private String senderName;

    /**
     * Sender level
     */
    @Column(nullable = false)
    private Integer senderLevel;

    /**
     * Receiver role ID
     */
    @Column(nullable = false)
    private Long receiverId;

    /**
     * Receiver name
     */
    @Column(nullable = false, length = 50)
    private String receiverName;

    /**
     * Request message
     */
    @Column(length = 200)
    private String message;

    /**
     * Request status
     * 0 = Pending
     * 1 = Accepted
     * 2 = Rejected
     * 3 = Expired
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0;

    /**
     * Process time
     */
    private LocalDateTime processedAt;

    /**
     * Request time
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if pending
     */
    public boolean isPending() {
        return status == 0;
    }

    /**
     * Accept request
     */
    public void accept() {
        this.status = 1;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Reject request
     */
    public void reject() {
        this.status = 2;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Expire request
     */
    public void expire() {
        this.status = 3;
        this.processedAt = LocalDateTime.now();
    }
}
