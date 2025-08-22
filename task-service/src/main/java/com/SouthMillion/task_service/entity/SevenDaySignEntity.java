package com.SouthMillion.task_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seven_day_sign")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SevenDaySignEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", unique = true, nullable = false, length = 64)
    private String playerId;

    @Column(name = "start_epoch", nullable = false)
    private Long startEpoch;

    @Column(name = "signed_mask", nullable = false)
    private Integer signedMask;

    @Column(name = "claimed_mask", nullable = false)
    private Integer claimedMask;

    @Column(name = "last_sign_date")
    private java.sql.Date lastSignDate; // chặn double sign trong 1 ngày
}