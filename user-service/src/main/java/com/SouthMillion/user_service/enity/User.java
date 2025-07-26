package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username; // Tài khoản

    @Column(nullable = false, length = 128)
    private String password; // Mật khẩu đã hash (BCrypt)

    @Column(length = 32)
    private String role = "ROLE_USER";
}