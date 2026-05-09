package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exclusive_gift")
public class ExclusiveGift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "gifts_json", nullable = false, columnDefinition = "TEXT")
    private String giftsJson; // JSON array of {endTimestamp, alreadyBuyTimes, seq}

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    @Transactional
    public void updateScoreAndReward(Long newScore, String rewardItem) {
        // Update score logic
        // Grant reward logic
    }
}