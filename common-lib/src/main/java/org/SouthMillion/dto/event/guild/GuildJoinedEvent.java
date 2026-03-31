package org.SouthMillion.dto.event.guild;

import lombok.*;

import java.time.Instant;

/**
 * Guild Joined Event - Published when a player is approved and joins a guild.
 * Kafka topic: guild.joined
 * Consumed by: analytics-service, task-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuildJoinedEvent {
    private String eventId;     // UUID for idempotency
    private Long roleId;        // Player who joined
    private Long guildId;       // Guild that was joined
    private String guildName;   // Name of the guild
    private Instant joinedAt;
}

