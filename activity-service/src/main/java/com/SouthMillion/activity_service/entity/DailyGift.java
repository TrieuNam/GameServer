package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Daily Gift (每日特惠) for RandActivity type 14.
 * Daily special gift shop with item purchase tracking.
 */
@Entity
@Table(name = "daily_gift")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyGift {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Purchase flags (购买标识): bitmask for purchased items */
    @Column(name = "buy_flag", nullable = false)
    private Long buyFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
