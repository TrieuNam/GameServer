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
@Table(name="pet_fight")
public class PetFightEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="role_id", nullable=false, unique=true, length=64)
    private String roleId;

    @Column(name="fight_indexes_json", columnDefinition = "json")
    private String fightIndexesJson;

    @UpdateTimestamp
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;
}