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

/** Player level (i18n: player.level) */
@Column(name = "level", nullable = false)
private Integer level;

/** Current task day (i18n: task.current_day) */
@Column(name = "cur_task_day", nullable = false)
private Integer curTaskDay;

/** Today's task finish status (i18n: task.today_completion) */
@Column(name = "today_task_finish", nullable = false)
private Integer todayTaskFinish;

/** JSON array of recharge task progress (i18n: task.recharge_progress) */
@Column(name = "chongzhi_task_proceed_json", nullable = false, columnDefinition = "TEXT")
private String chongzhiTaskProceedJson;

/** JSON array of friend task progress (i18n: task.friend_progress) */
@Column(name = "friend_task_proceed_json", nullable = false, columnDefinition = "TEXT")
private String friendTaskProceedJson;

/** JSON array of receive reward flags (i18n: task.receive_reward_flags) */
@Column(name = "receive_rewards_flag_json", nullable = false, columnDefinition = "TEXT")
private String receiveRewardsFlagJson;

@UpdateTimestamp
@Column(name = "updated_at")
private LocalDateTime updatedAt;
}
