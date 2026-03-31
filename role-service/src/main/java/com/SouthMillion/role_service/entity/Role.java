package com.SouthMillion.role_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "role",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_user_name", columnNames = {"user_id", "name"}))
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "exp", nullable = false)
    private long exp;

    @Column(name = "hp", nullable = false)
    private long hp;

    @Column(name = "attack_value", nullable = false)
    private long attackValue;

    @Column(name = "defense_value", nullable = false)
    private long defenseValue;

    @Column(name = "speed", nullable = false)
    private int speed;

    @Column(name = "cap")
    private Long cap;

    @Column(name = "head_pic_id")
    private Integer headPicId;

    @Column(name = "title_id")
    private Integer titleId;

    @Column(name = "knight_level")
    private Integer knightLevel;

    @Column(name = "head_char", length = 255)
    private String headChar;

    @Column(name = "guild_name", length = 64)
    private String guildName;

    @Column(name = "notice_time", nullable = false)
    private Long noticeTime = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (level <= 0) level = 1;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}