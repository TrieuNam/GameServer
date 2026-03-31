package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 39: 领地礼包 (Territory Gift)
 * Territory-based gift pack system with type rotation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "territory_gift")
public class TerritoryGift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "buy_count", nullable = false)
    private Integer buyCount; // Number of purchases

    @Column(name = "now_type", nullable = false)
    private Integer nowType; // Current gift type

    @Column(name = "next_time", nullable = false)
    private Integer nextTime; // Next refresh timestamp

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
