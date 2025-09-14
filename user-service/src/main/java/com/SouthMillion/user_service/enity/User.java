package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.*;
import org.SouthMillion.dto.user.UserStatus;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "uk_users_account", columnList = "account", unique = true),
                @Index(name = "uk_users_username", columnList = "username", unique = true)
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false, unique = true, length = 64)
    private String account;    // tài khoản đăng nhập (unique)

    @Column(nullable = false, length = 64)
    private String username;   // tên hiển thị (cũng unique theo DB)

    @Column(name = "pass_hash", nullable = false, length = 100)
    private String passHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;
}