package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * LianChong ZengLi (连充赠礼) for RandActivity type 32.
 * Consecutive recharge gift activity with daily tasks.
 */
@Entity
@Table(name = "lianchong_zengli")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LianChongZengLi {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Player level (玩家等级) */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** Current task day (当前任务天数) */
    @Column(name = "cur_task_day", nullable = false)
    private Integer curTaskDay;

    /** Today's task finish status (今日任务完成) */
    @Column(name = "today_task_finish", nullable = false)
    private Integer todayTaskFinish;

    /** JSON array of recharge task progress (充值任务进度) */
    @Column(name = "chongzhi_task_proceed_json", nullable = false, columnDefinition = "TEXT")
    private String chongzhiTaskProceedJson;

    /** JSON array of friend task progress (好友任务进度) */
    @Column(name = "friend_task_proceed_json", nullable = false, columnDefinition = "TEXT")
    private String friendTaskProceedJson;

    /** JSON array of receive reward flags (领取奖励标识) */
    @Column(name = "receive_rewards_flag_json", nullable = false, columnDefinition = "TEXT")
    private String receiveRewardsFlagJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
