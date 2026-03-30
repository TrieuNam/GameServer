package com.SouthMillion.guild_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.guild.GuildCreatedEvent;
import org.SouthMillion.dto.event.guild.GuildJoinedEvent;
import org.SouthMillion.dto.event.guild.GuildLeftEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes guild-related events to Kafka.
 * Topics:
 *   - guild.created  → consumed by analytics-service, task-service
 *   - guild.joined   → consumed by analytics-service, task-service
 *   - guild.left     → consumed by analytics-service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuildEventPublisher {

    static final String TOPIC_CREATED = "guild.created";
    static final String TOPIC_JOINED  = "guild.joined";
    static final String TOPIC_LEFT    = "guild.left";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish event when a guild is created.
     * Also publishes a guild.joined event for the founding leader.
     */
    public void publishGuildCreated(Long roleId, Long guildId, String guildName) {
        try {
            GuildCreatedEvent event = GuildCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .roleId(roleId)
                    .guildId(guildId)
                    .guildName(guildName)
                    .createdAt(Instant.now())
                    .build();
            kafkaTemplate.send(TOPIC_CREATED, String.valueOf(guildId), event);
            log.info("[GuildEvent] Published guild.created: guildId={}, roleId={}", guildId, roleId);
        } catch (Exception e) {
            log.warn("[GuildEvent] Failed to publish guild.created: guildId={}, error={}", guildId, e.getMessage());
        }

        // The creator also "joins" the guild
        publishGuildJoined(roleId, guildId, guildName);
    }

    /**
     * Publish event when a player joins a guild (approved application).
     */
    public void publishGuildJoined(Long roleId, Long guildId, String guildName) {
        try {
            GuildJoinedEvent event = GuildJoinedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .roleId(roleId)
                    .guildId(guildId)
                    .guildName(guildName)
                    .joinedAt(Instant.now())
                    .build();
            kafkaTemplate.send(TOPIC_JOINED, String.valueOf(guildId), event);
            log.info("[GuildEvent] Published guild.joined: guildId={}, roleId={}", guildId, roleId);
        } catch (Exception e) {
            log.warn("[GuildEvent] Failed to publish guild.joined: guildId={}, error={}", guildId, e.getMessage());
        }
    }

    /**
     * Publish event when a player leaves or is kicked from a guild.
     * @param reason "LEFT", "KICKED", or "DISBANDED"
     */
    public void publishGuildLeft(Long roleId, Long guildId, String guildName, String reason) {
        try {
            GuildLeftEvent event = GuildLeftEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .roleId(roleId)
                    .guildId(guildId)
                    .guildName(guildName)
                    .reason(reason)
                    .leftAt(Instant.now())
                    .build();
            kafkaTemplate.send(TOPIC_LEFT, String.valueOf(guildId), event);
            log.info("[GuildEvent] Published guild.left: guildId={}, roleId={}, reason={}", guildId, roleId, reason);
        } catch (Exception e) {
            log.warn("[GuildEvent] Failed to publish guild.left: guildId={}, error={}", guildId, e.getMessage());
        }
    }
}

