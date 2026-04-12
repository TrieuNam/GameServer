package com.SouthMillion.rune_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Lưu trạng thái tháp/vòng quay của từng player.
 * Tương ứng với RuneParam trong C++ server gốc.
 */
@Entity
@Table(name = "rune_tower_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuneTowerState {

    /** role_id là PK trực tiếp (1 row per player) */
    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** Cấp tháp đã thông qua (0 = chưa qua cấp nào) */
    @Column(name = "tower_level", nullable = false)
    @Builder.Default
    private Integer towerLevel = 0;

    /** Số lần quay vòng quay còn lại */
    @Column(name = "turntable_num", nullable = false)
    @Builder.Default
    private Integer turntableNum = 0;

    /**
     * Bitmask ô đã nhận thưởng trong vòng hiện tại.
     * Bit thứ i tương ứng cur_index = i+1.
     */
    @Column(name = "turntable_flag", nullable = false)
    @Builder.Default
    private Integer turntableFlag = 0;

    /** Vòng hiện tại của vòng quay (0-indexed) */
    @Column(name = "turntable_round", nullable = false)
    @Builder.Default
    private Integer turntableRound = 0;

    /** 0 = chưa nhận thưởng hàng ngày, 1 = đã nhận */
    @Column(name = "daily_reward", nullable = false)
    @Builder.Default
    private Integer dailyReward = 0;

    /** Index phần thưởng thông quan đã nhận tiếp theo */
    @Column(name = "pass_reward_index", nullable = false)
    @Builder.Default
    private Integer passRewardIndex = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
