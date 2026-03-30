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
 * Pet Entity
 * Represents a pet in a user's collection
 */
@Entity
@Table(name = "pet_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(Pet.PetId.class)
public class Pet {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Column(name = "pet_index", nullable = false)
    private Integer petIndex;

    @Column(name = "pet_id", nullable = false)
    private Integer petId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "exp", nullable = false)
    private Long exp = 0L;

    @Column(name = "`order`", nullable = false)
    private Integer order = 1;

    @Convert(converter = IntListConverter.class)
    @Column(name = "skill_list", length = 255)
    private List<Integer> skillList = new ArrayList<>();

    @Convert(converter = IntListConverter.class)
    @Column(name = "gem_item_id", length = 255)
    private List<Integer> gemItemId = new ArrayList<>();

    @Convert(converter = IntListConverter.class)
    @Column(name = "ts_gem_index", length = 255)
    private List<Integer> tsGemIndex = new ArrayList<>();

    @Column(name = "ts_gem_pos_1", nullable = false)
    private Integer tsGemPos1 = null;

    @Column(name = "ts_gem_pos_2", nullable = false)
    private Integer tsGemPos2 = null;

    @Column(name = "gem_pos_1", nullable = false)
    private Integer gemPos1 = null;

    @Column(name = "gem_pos_2", nullable = false)
    private Integer gemPos2 = null;

    @Column(name = "gem_pos_3", nullable = false)
    private Integer gemPos3 = null;

    @Column(name = "gem_pos_4", nullable = false)
    private Integer gemPos4 = null;

    @Column(name = "skill_lock_flag", nullable = false)
    private Integer skillLockFlag = 0;

    @Column(name = "cloth_id", nullable = false)
    private Integer clothId = 0;

    @Column(name = "capability", nullable = false)
    private Long capability = 0L;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Composite primary key for Pet
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetId implements Serializable {
        private String userId;
        private Integer petIndex;
    }

    /**
     * Initialize default skill list (6 slots, -1 = locked)
     */
    public void initializeSkills(List<Integer> initialSkills) {
        this.skillList = new ArrayList<>(6);
        if (initialSkills != null && !initialSkills.isEmpty()) {
            this.skillList.addAll(initialSkills);
        }
        // Fill remaining slots with -1 (locked)
        while (this.skillList.size() < 6) {
            this.skillList.add(-1);
        }
    }

    /**
     * Initialize default gem slots (4 normal gems)
     */
    public void initializeGems() {
        this.gemItemId = new ArrayList<>(List.of(0, 0, 0, 0));
    }

    /**
     * Initialize default special gem slots (2 special gems)
     */
    public void initializeTSGems() {
        this.tsGemIndex = new ArrayList<>(List.of(0, 0));
    }
}
