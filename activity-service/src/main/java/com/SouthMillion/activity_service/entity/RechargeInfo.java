package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Recharge tracking (充值信息) for RandActivity type 1.
 * Tracks total recharge amount and daily recharge.
 */
@Entity
@Table(name = "recharge_info")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RechargeInfo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Total historical recharge amount (历史充值金额) in cents/points */
    @Column(name = "history_chongzhi", nullable = false)
    private Long historyChongzhi;

    /** Total number of recharge transactions (历史充值次数) */
    @Column(name = "history_chongzhi_count", nullable = false)
    private Integer historyChongzhiCount;

    /** Today's recharge amount (今日充值) */
    @Column(name = "today_chongzhi", nullable = false)
    private Integer todayChongzhi;

    /** Last recharge date (for daily reset logic) */
    @Column(name = "last_recharge_date")
    private LocalDateTime lastRechargeDate;

    /** JSON array of reward claim times (充值奖励领取次数) [tier1Times, tier2Times, ...] */
    @Column(name = "chongzhi_reward_times", nullable = false, columnDefinition = "TEXT")
    private String chongzhiRewardTimes;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
