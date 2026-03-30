package org.SouthMillion.dto.event.guild;

import lombok.*;

import java.time.Instant;

/**
 * Guild Left Event - Published when a player leaves or is kicked from a guild.
 * Kafka topic: guild.left
 * Consumed by: analytics-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuildLeftEvent {
    private String eventId;     // UUID for idempotency
    private Long roleId;        // Player who left
    private Long guildId;       // Guild that was left
    private String guildName;   // Name of the guild
    private String reason;      // "LEFT", "KICKED", "DISBANDED"
    private Instant leftAt;
}

