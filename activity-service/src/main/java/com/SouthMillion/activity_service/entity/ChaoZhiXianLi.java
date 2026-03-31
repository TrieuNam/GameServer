package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ChaoZhiXianLi (超值献礼) for RandActivity type 28.
 * Premium value gift activity with item purchase tracking.
 */
@Entity
@Table(name = "chaozhi_xianli")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChaoZhiXianLi {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Player level (玩家等级) */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** Purchase flags (购买标识): bitmask */
    @Column(name = "buy_mark", nullable = false)
    private Integer buyMark;

    /** JSON array of item purchase counts (物品数量) */
    @Column(name = "item_num_json", nullable = false, columnDefinition = "TEXT")
    private String itemNumJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
