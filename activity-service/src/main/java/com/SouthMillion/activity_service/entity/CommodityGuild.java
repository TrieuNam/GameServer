package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Commodity Guild (商品行会) for RandActivity type 15.
 * Guild-based discount shop with purchase tracking.
 */
@Entity
@Table(name = "commodity_guild")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CommodityGuild {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Current discount percentage (当前折扣) */
    @Column(name = "cur_discount", nullable = false)
    private Integer curDiscount;

    /** Open level requirement (开放等级) */
    @Column(name = "open_level", nullable = false)
    private Integer openLevel;

    /** JSON array of purchased times per item (购买次数) */
    @Column(name = "purchased_times_json", nullable = false, columnDefinition = "TEXT")
    private String purchasedTimesJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
