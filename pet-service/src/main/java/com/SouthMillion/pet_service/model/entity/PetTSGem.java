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
import java.util.ArrayList;
import java.util.List;

/**
 * Special Gem Entity (TS Gem - Tian Shi Gem)
 * Gems with random attributes that can be equipped on pets
 */
@Entity
@Table(name = "pet_ts_gem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(PetTSGem.PetTSGemId.class)
public class PetTSGem {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Column(name = "gem_index", nullable = false)
    private Integer gemIndex;

    @Column(name = "gem_level", nullable = false)
    private Integer gemLevel;

    @Column(name = "pet_index", nullable = false)
    private Integer petIndex = 0;

    @Convert(converter = IntListConverter.class)
    @Column(name = "attr_type", length = 255)
    private List<Integer> attrType = new ArrayList<>();

    @Convert(converter = IntListConverter.class)
    @Column(name = "attr_value", length = 255)
    private List<Integer> attrValue = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetTSGemId implements Serializable {
        private String userId;
        private Integer gemIndex;
    }

    /**
     * Initialize default attributes (4 slots)
     */
    public void initializeAttributes() {
        this.attrType = new ArrayList<>(List.of(0, 0, 0, 0));
        this.attrValue = new ArrayList<>(List.of(0, 0, 0, 0));
    }

    /**
     * Check if gem is equipped on any pet
     */
    public boolean isEquipped() {
        return this.petIndex != null && this.petIndex > 0;
    }
}
