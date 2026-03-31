package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores per-role DuoBao (夺宝) state for one duobao type.
 * Two rows per role: duoBaoType=1 (初级 basic) and duoBaoType=2 (高级 advanced).
 */
@Entity
@Table(name = "duobao_data",
    uniqueConstraints = @UniqueConstraint(name = "uq_duobao_role_type", columnNames = {"role_id", "duobao_type"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DuoBaoData {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** 1 = 初级 (basic), 2 = 高级 (advanced) */
    @Column(name = "duobao_type", nullable = false)
    private Integer duoBaoType;

    /** Accumulated integral (积分) from draws */
    @Column(name = "integral", nullable = false)
    private Integer integral;

    /** Bitmask of claimed integral rewards (积分奖励领取标识) */
    @Column(name = "fetch_flag", nullable = false)
    private Integer fetchFlag;

    /** Remaining free refreshes (免费刷新次数) */
    @Column(name = "free_refresh_num", nullable = false)
    private Integer freeRefreshNum;

    /** Unix timestamp (seconds) when the next free refresh becomes available */
    @Column(name = "free_refresh_time", nullable = false)
    private Integer freeRefreshTime;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
