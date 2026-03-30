package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Weekend LianChong (周末连充) for RandActivity type 34.
 * Weekend consecutive recharge activity.
 */
@Entity
@Table(name = "weekend_lianchong")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WeekendLianChong {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Player level (玩家等级) */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** Daily recharge amount for current day (今日充值数量) */
    @Column(name = "day_chongzhi_num", nullable = false)
    private Long dayChongzhiNum;

    /** JSON array of recharge amounts per day (每日充值数量) */
    @Column(name = "chongzhi_num_json", nullable = false, columnDefinition = "TEXT")
    private String chongzhiNumJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
