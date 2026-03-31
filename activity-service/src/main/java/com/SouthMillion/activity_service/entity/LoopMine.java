package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "ra_loop_mine")
public class LoopMine {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    /** Current mine level (1-based) */
    @Column(name = "mine_level", nullable = false)
    private Integer mineLevel = 1;

    /** Ore collected in current cycle */
    @Column(name = "ore_count", nullable = false)
    private Integer oreCount = 0;

    /** Total cycles completed */
    @Column(name = "cycle_count", nullable = false)
    private Integer cycleCount = 0;

    /** Daily mining attempts used */
    @Column(name = "daily_attempts", nullable = false)
    private Integer dailyAttempts = 0;

    @Column(name = "last_attempt_date")
    private LocalDate lastAttemptDate;

    /** fetch_flag bitmask for cycle rewards */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag = 0L;
}
