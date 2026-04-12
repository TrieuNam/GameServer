package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Cave Loot (山洞夺宝) for RandActivity type 19.
 * Cave treasure lottery system with recharge milestones and task rewards.
 */
@Entity
@Table(name = "cave_loot")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CaveLoot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Total lottery draw count (抽奖总次数); mapped to legacy DB column free_num */
    @Column(name = "free_num", nullable = false)
    private Integer lotteryCount;

    /** Recharge milestone rewards claimed bitmask (充值奖励标识); mapped to legacy DB column chongzhi_fetch_flag */
    @Column(name = "chongzhi_fetch_flag", nullable = false)
    private Integer chongzhiReceiveFlag;

    /** Legacy task fetch bitmask — superseded by rewardReceiveJson; kept for schema compat */
    @Column(name = "task_fetch_flag", nullable = false)
    private Long taskFetchFlag;

    /** Player level when activity started (开服等级) */
    @Column(name = "open_level")
    private Integer openLevel;

    /** Cumulative recharge amount (累计充值) */
    @Column(name = "total_chongzhi")
    private Integer totalChongzhi;

    /** Shop item purchase counts JSON array indexed by seq */
    @Column(name = "buy_times_json", columnDefinition = "TEXT")
    private String buyTimesJson;

    /** Task progress JSON array indexed by task_type */
    @Column(name = "task_param_json", columnDefinition = "TEXT")
    private String taskParamJson;

    /** Task reward claimed count JSON array indexed by task_type */
    @Column(name = "reward_receive_json", columnDefinition = "TEXT")
    private String rewardReceiveJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
