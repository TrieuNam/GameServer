package com.SouthMillion.mail_service.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mail Entity
 * 
 * Types:
 * 1 = System Mail
 * 2 = Player Mail
 * 3 = Reward Mail
 * 4 = Notice Mail
 */
@Entity
@Table(name = "mail", indexes = {
    @Index(name = "idx_mail_receiver", columnList = "receiver_id"),
    @Index(name = "idx_mail_status", columnList = "is_deleted, is_read"),
    @Index(name = "idx_mail_expire", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer type; // 1=System, 2=Player, 3=Reward, 4=Notice

    @Column(length = 50)
    private Long senderId;

    @Column(length = 50)
    private String senderName;

    @Column(nullable = false, length = 50)
    private Long receiverId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isClaimedAttachment = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime readAt;

    @Column
    private LocalDateTime claimedAt;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void markAsClaimedAttachment() {
        this.isClaimedAttachment = true;
        this.claimedAt = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean hasAttachments() {
        // Will be checked via MailAttachment repository
        return true;
    }
}
