package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Weekend Recharge (周末累充) for RandActivity type 18.
 * Weekend accumulated recharge milestone rewards.
 */
@Entity
@Table(name = "weekend_recharge")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WeekendRecharge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Open level requirement (开放等级) */
    @Column(name = "open_level", nullable = false)
    private Integer openLevel;

    /** Total recharge amount for weekend (累计充值) */
    @Column(name = "total_chongzhi", nullable = false)
    private Integer totalChongzhi;

    /** Milestone rewards received (领取标识): bitmask */
    @Column(name = "receive_flag", nullable = false)
    private Integer receiveFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
