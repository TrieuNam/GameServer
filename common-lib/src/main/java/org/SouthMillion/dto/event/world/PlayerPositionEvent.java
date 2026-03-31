package org.SouthMillion.dto.event.world;

import lombok.*;

import java.time.Instant;

/**
 * Player Position Event - Published when player moves significantly
 * Consumed by: analytics, anti-cheat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerPositionEvent {
    private String eventId;
    private Long roleId;
    private Integer zoneId;
    
    // Position
    private Float x;
    private Float y;
    private Float z;
    private Float rotation;
    
    private String movementState; // IDLE, WALKING, RUNNING, JUMPING, FLYING, TELEPORT
    private String eventType;     // MOVE, ENTER_ZONE, LEAVE_ZONE, TELEPORT
    private Instant timestamp;
}
