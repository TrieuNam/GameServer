package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

@Entity
@Table(name = "user_account",
        indexes = {
                @Index(name="uk_username", columnList = "username", unique = true),
                @Index(name="idx_spid_ext", columnList = "spid, external_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class UserAccount {

    @Id
    @Column(length = 50, nullable = false)
    private String id;                   // UUID/ULID (string)

    @Column(length = 64, nullable = false, unique = true)
    private String username;             // tên đăng nhập (có thể auto-generate cho H5)

    @Column(length = 100, nullable = false)
    private String passwordHash;         // BCrypt hash; với H5 có thể đặt placeholder cố định

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";    // ACTIVE | BANNED

    // Dấu vết từ H5/channel
    @Column(length = 32)
    private String spid;                 // kênh

    @Column(length = 128)
    private String externalId;           // userId từ H5 hoặc openid, unionid...

    @Column(length = 32)
    private String device;               // device string từ H5 (tùy chọn)

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastLoginAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}