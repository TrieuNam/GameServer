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
 * Pet Remains Entity
 * Pet relics/remains that provide bonuses
 */
@Entity
@Table(name = "pet_remains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(PetRemains.PetRemainsId.class)
public class PetRemains {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Column(name = "remains_index", nullable = false)
    private Integer remainsIndex;

    @Column(name = "remains_id", nullable = false)
    private Integer remainsId;

    @Column(name = "grade", nullable = false)
    private Integer grade = 1;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "exp", nullable = false)
    private Long exp = 0L;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetRemainsId implements Serializable {
        private String userId;
        private Integer remainsIndex;
    }
}
