package com.SouthMillion.worlds_ervice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldBossDTO {
    private Long bossId;
    private String bossName;
    private Integer bossLevel;
    private Long currentHp;
    private Long maxHp;
    private Integer hpPercentage;
    private String status;
    private String location;
    private Instant spawnTime;
    private String defeatedBy;
}
