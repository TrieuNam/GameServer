package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 35: 广告权益 (Advertisement Equity / Knights Welfare)
 * Premium subscription for advertisement-based benefits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "advertisement_equity")
public class AdvertisementEquity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "is_buy", nullable = false)
    private Integer isBuy; // 0=not purchased, 1=active subscription

    @Column(name = "fetch_flag", nullable = false)
    private Integer fetchFlag; // Bitmask for claimed daily rewards

    @Column(name = "refresh_time", nullable = false)
    private Integer refreshTime; // Next refresh timestamp

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
