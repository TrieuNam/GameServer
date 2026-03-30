package com.SouthMillion.gameworld_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerPosition {
    private Long playerId;
    private Integer mapId;
    private Float x;
    private Float y;
    private Float z;
    private Float direction;  // 0-360 degrees
    private Long timestamp;
    private String state;     // IDLE, MOVING, FIGHTING, DEAD
}
