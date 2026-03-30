package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type 38: 天选之礼 (TianXuan Gift / Chosen Gift)
 * Time-limited gift shop with free gift and purchasable items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tianxuan_gift")
public class TianxuanGift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "role_level", nullable = false)
    private Integer roleLevel; // Player level

    @Column(name = "gift_open_timestamp", nullable = false)
    private Integer giftOpenTimestamp; // When gift shop becomes available

    @Column(name = "gift_close_timestamp", nullable = false)
    private Integer giftCloseTimestamp; // When gift shop closes

    @Column(name = "gift_cd_end_timestamp", nullable = false)
    private Integer giftCdEndTimestamp; // Cooldown end time

    @Column(name = "has_fetch_free_gift", nullable = false)
    private Boolean hasFetchFreeGift; // Has claimed free gift

    @Column(name = "group_id", nullable = false)
    private Integer groupId; // Gift group configuration ID

    @Column(name = "accumulated_chongzhi_num", nullable = false)
    private Integer accumulatedChongzhiNum; // Total recharge amount

    @Column(name = "gifts_json", nullable = false, columnDefinition = "TEXT")
    private String giftsJson; // JSON array of {seq, buyNum}

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
