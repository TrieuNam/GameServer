package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Month Card (月卡) for RandActivity type 16.
 * Each row represents one card type subscription for a role.
 */
@Entity
@Table(name = "month_card",
    uniqueConstraints = @UniqueConstraint(name = "uq_month_card_role_type", columnNames = {"role_id", "card_type"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthCard {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** Card type/tier (卡类型: 1=basic, 2=premium, etc.) */
    @Column(name = "card_type", nullable = false)
    private Integer cardType;

    /** Bitmask of daily rewards claimed (领取标记) */
    @Column(name = "fetch_mark", nullable = false)
    private Integer fetchMark;

    /** Remaining days (剩余天数) */
    @Column(name = "have_days", nullable = false)
    private Integer haveDays;

    /** Expiration timestamp (结束时间戳) in seconds */
    @Column(name = "end_timestamp", nullable = false)
    private Integer endTimestamp;

    /** 0=not first purchase, 1=first purchase bonus claimed (首次购买标记) */
    @Column(name = "first_buy_mark", nullable = false)
    private Integer firstBuyMark;

    /** Purchase level/tier (购买等级) */
    @Column(name = "buy_level", nullable = false)
    private Integer buyLevel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
