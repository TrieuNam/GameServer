package com.SouthMillion.role_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Thiên phú kỹ năng (passive talent) của nhân vật.
 *
 * <p>Tương ứng với PB_RoleTalentInfoPro (msgskill.proto).
 * <ul>
 *   <li>skill_id    — id thiên phú (tham chiếu passive_skill.json)</li>
 *   <li>skill_level — cấp độ thiên phú</li>
 * </ul>
 */
@Entity
@Table(
    name = "role_talent",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_role_talent",
        columnNames = {"role_id", "skill_id"}
    ),
    indexes = @Index(name = "idx_role_talent_role", columnList = "role_id")
)
@Getter
@Setter
public class RoleTalent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "skill_id", nullable = false)
    private Integer skillId;

    @Column(name = "skill_level", nullable = false)
    private Integer skillLevel = 1;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = Instant.now();
        if (skillLevel == null || skillLevel < 1) skillLevel = 1;
    }
}

