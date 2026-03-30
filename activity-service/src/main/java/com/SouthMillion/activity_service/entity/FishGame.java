package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "ra_fish_game")
public class FishGame {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "fish_count", nullable = false)
    private Integer fishCount = 0;

    @Column(name = "daily_attempts", nullable = false)
    private Integer dailyAttempts = 0;

    @Column(name = "last_attempt_date")
    private LocalDate lastAttemptDate;

    @Column(name = "total_fish_caught", nullable = false)
    private Integer totalFishCaught = 0;

    /** fetch_flag bitmask: each bit = reward tier claimed */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag = 0L;
}
