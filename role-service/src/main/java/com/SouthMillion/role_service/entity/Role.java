package com.SouthMillion.role_service.entity;

import com.SouthMillion.role_service.utils.IdGen;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "role")
@Getter
@Setter
public class Role {

    @Id
    @Column(name = "role_id", length = 26)
    private String roleId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private long exp; // exp trong cấp hiện tại

    @Column(nullable = false, name = "hp")
    private long hp;

    @Column(nullable = false, name = "attack_value")
    private long attack;

    @Column(nullable = false, name = "defense_value")
    private long defense;

    @Column(nullable = false)
    private int speed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (roleId == null || roleId.isBlank()) {
            roleId = IdGen.ulid(); // ULID 26 chars
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}