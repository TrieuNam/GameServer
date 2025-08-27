package com.SouthMillion.pet_service.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pet_role",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_petindex", columnNames = {"role_id", "pet_index"}))
public class PetRoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, length = 64)
    private String roleId;

    @Column(name = "pet_index", nullable = false)
    private Integer petIndex;

    @Column(name = "pet_id", nullable = false)
    private Integer petId;

    @Column(nullable = false)
    private Integer level = 1;
    @Column(nullable = false)
    private Long exp = 0L;
    @Column(name = "pet_order", nullable = false)
    private Integer order = 0;

    @Column(name = "skill_lock_flag", nullable = false)
    private Integer skillLockFlag = 0;

    @Column(columnDefinition = "json")
    private String skillsJson;
    @Column(columnDefinition = "json")
    private String gemsJson;
    @Column(columnDefinition = "json")
    private String tsGemsJson;

    @Version
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
