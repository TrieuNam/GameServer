package com.SouthMillion.role_service.entity;

import com.SouthMillion.role_service.utils.IdGen;
import com.SouthMillion.role_service.utils.MailItemsConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.dto.role.mail.MailItem;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "mail")
@Getter
@Setter
public class Mail {

    @Id
    @Column(name = "mail_id", nullable = false, length = 26)
    private String mailId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Convert(converter = MailItemsConverter.class)
    @Column(name = "items", columnDefinition = "json")
    private List<MailItem> items;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "is_fetched", nullable = false)
    private boolean fetched;

    @Column(name = "expire_at")
    private Instant expireAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (mailId == null || mailId.isBlank()) mailId = IdGen.ulid();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean hasReward() {
        return items != null && !items.isEmpty();
    }

    public boolean isExpired() {
        return expireAt != null && Instant.now().isAfter(expireAt);
    }
}