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

    /** Number of free lottery attempts used (免费次数) */
    @Column(name = "free_num", nullable = false)
    private Integer freeNum;

    /** Recharge milestone rewards claimed (充值奖励标识): bitmask */
    @Column(name = "chongzhi_fetch_flag", nullable = false)
    private Integer chongzhiFetchFlag;

    /** Task completion rewards claimed (任务奖励标识): bitmask */
    @Column(name = "task_fetch_flag", nullable = false)
    private Long taskFetchFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
