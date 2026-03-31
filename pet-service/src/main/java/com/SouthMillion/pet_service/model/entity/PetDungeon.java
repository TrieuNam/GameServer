package com.SouthMillion.pet_service.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores per-user pet dungeon (宠物副本) progress.
 * One row per user.
 *
 * passLevel  — highest cleared level (0 = none cleared)
 * fetchFlag  — bitmask of claimed rewards (bit i = level i+1 reward claimed)
 */
@Entity
@Table(name = "pet_dungeon",
    uniqueConstraints = @UniqueConstraint(name = "uq_pet_dungeon_user", columnNames = "user_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PetDungeon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    /** Highest cleared level */
    @Column(name = "pass_level", nullable = false)
    private Integer passLevel;

    /** Bitmask of claimed dungeon rewards */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
