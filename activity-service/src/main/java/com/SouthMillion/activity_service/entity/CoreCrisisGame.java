package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "ra_core_crisis")
public class CoreCrisisGame {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    /** Current boss stage (1-based) */
    @Column(name = "stage", nullable = false)
    private Integer stage = 1;

    /** Total damage dealt to boss */
    @Column(name = "total_damage", nullable = false)
    private Long totalDamage = 0L;

    /** Number of attempts today */
    @Column(name = "daily_attempts", nullable = false)
    private Integer dailyAttempts = 0;

    /** fetch_flag bitmask for stage rewards */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag = 0L;

    /** Unix timestamp when event ends */
    @Column(name = "end_timestamp", nullable = false)
    private Integer endTimestamp = 0;

    @Column(name = "is_buy", nullable = false)
    private Integer isBuy = 0;
}
