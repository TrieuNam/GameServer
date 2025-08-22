package com.southMillion.report_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_boss_kill")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BossKillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "boss_kill_count", nullable = false)
    private Integer bossKillCount;
}