package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * FaZhen Gala (法阵盛典) for RandActivity type 24.
 * FaZhen (magic circle) festival activity with task and gift tracking.
 */
@Entity
@Table(name = "fazhen_gala")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FaZhenGala {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Player level (玩家等级) */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** Activity end timestamp (活动结束时间) */
    @Column(name = "end_timestamp", nullable = false)
    private Integer endTimestamp;

    /** Reward fetch flags (领取标识): bitmask */
    @Column(name = "fetch_flag", nullable = false)
    private Integer fetchFlag;

    /** JSON array of task completion counts (任务完成数) */
    @Column(name = "task_num_json", nullable = false, columnDefinition = "TEXT")
    private String taskNumJson;

    /** JSON array of gift purchase counts (礼包购买数) */
    @Column(name = "gift_num_json", nullable = false, columnDefinition = "TEXT")
    private String giftNumJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
