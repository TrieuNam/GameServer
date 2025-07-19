package com.SouthMillion.user_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;
    @Column(unique = true)
    private String email;
    private String avatar;
    private String nickname;
    private String gender;
    private String language;
    private String status; // "active", "banned"
    private String blockReason;
    private LocalDateTime blockUntil;
    private String googleId;
    private String facebookId;
    private LocalDateTime lastLogin;
    private String deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long coin;
    private long gem;

    private Boolean inBattle;
    private Long currentBattleId;

    private Boolean online;
    private LocalDateTime lastOnline;

    private Integer accountType;
    private Integer fcmFlag;
    private String openid;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Role> roles;
}