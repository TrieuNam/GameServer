package com.SouthMillion.crafting_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_crafting",
        indexes = {
            @Index(name = "idx_user_crafting_role", columnList = "roleId"),
            @Index(name = "idx_user_crafting_status", columnList = "status"),
            @Index(name = "idx_user_crafting_end_time", columnList = "endTime")
        })
@Getter @Setter
public class UserCrafting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, length = 64)
    private String roleId;

    @Column(name = "recipe_id", nullable = false)
    private Integer recipeId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
