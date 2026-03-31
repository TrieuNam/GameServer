package com.SouthMillion.pet_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Pet Fight Index Entity
 * Tracks which pets are currently active in battle
 */
@Entity
@Table(name = "pet_fight_index")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PetFightIndex {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "fight_pet_index", nullable = false)
    private Integer fightPetIndex = 0;

    @Column(name = "fight_pet_index2", nullable = false)
    private Integer fightPetIndex2 = 0;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
