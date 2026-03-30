package com.SouthMillion.pet_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Pet Cloth Entity
 * Clothing/skins that can be equipped on pets
 */
@Entity
@Table(name = "pet_cloth")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(PetCloth.PetClothId.class)
public class PetCloth {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Column(name = "cloth_id", nullable = false)
    private Integer clothId;

    @Column(name = "level", nullable = false)
    private Integer level = 0;

    @Column(name = "pet_index", nullable = false)
    private Integer petIndex = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetClothId implements Serializable {
        private String userId;
        private Integer clothId;
    }

    /**
     * Check if clothing is equipped on any pet
     */
    public boolean isEquipped() {
        return this.petIndex != null && this.petIndex > 0;
    }
}
