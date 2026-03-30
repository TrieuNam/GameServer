package org.SouthMillion.dto.event.guild;

import lombok.*;

import java.time.Instant;

/**
 * Guild Created Event - Published when a player creates a new guild.
 * Kafka topic: guild.created
 * Consumed by: analytics-service, task-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuildCreatedEvent {
    private String eventId;     // UUID for idempotency
    private Long roleId;        // Player who created the guild
    private Long guildId;       // Newly created guild ID
    private String guildName;   // Name of the new guild
    private Instant createdAt;
}

