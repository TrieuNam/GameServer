package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "ra_ming_xiang")
public class MingXiang {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    /** Fate sign index (1-12, zodiac/astrology type) */
    @Column(name = "sign_index", nullable = false)
    private Integer signIndex = 0;

    /** Fortune level unlocked */
    @Column(name = "fortune_level", nullable = false)
    private Integer fortuneLevel = 0;

    /** Total divinations performed */
    @Column(name = "divination_count", nullable = false)
    private Integer divinationCount = 0;

    /** fetch_flag bitmask for fortune rewards */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag = 0L;

    /** Unix timestamp of last divination */
    @Column(name = "last_divination_time", nullable = false)
    private Long lastDivinationTime = 0L;
}
