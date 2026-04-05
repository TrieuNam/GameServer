package com.SouthMillion.analytics_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.arena.ArenaMatchEndEvent;
import org.SouthMillion.dto.event.combat.CombatResultEvent;
import org.SouthMillion.dto.event.guild.GuildCreatedEvent;
import org.SouthMillion.dto.event.guild.GuildJoinedEvent;
import org.SouthMillion.dto.event.guild.GuildLeftEvent;
import org.SouthMillion.dto.event.task.TaskProgressEvent;
import org.SouthMillion.dto.event.trial.TrialCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    static final String TOPIC_TASK_PROGRESS = "task.progress.update";

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: publish TaskProgressEvent → task-service
    // ─────────────────────────────────────────────────────────────────────────

    private void publishTaskProgress(Long roleId, String taskKey, int delta, String source) {
        try {
            TaskProgressEvent event = TaskProgressEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .roleId(roleId)
                    .taskKey(taskKey)
                    .progressDelta(delta)
                    .source(source)
                    .occurredAt(Instant.now())
                    .build();
                    log.info("[TaskProgress][analytics] publishing eventId={} roleId={} taskKey={} delta={} source={} topic={}",
                        event.getEventId(), roleId, taskKey, delta, source, TOPIC_TASK_PROGRESS);
            kafkaTemplate.send(TOPIC_TASK_PROGRESS, String.valueOf(roleId), event);
                    log.info("[TaskProgress][analytics] published eventId={} roleId={} taskKey={} delta={}",
                        event.getEventId(), roleId, taskKey, delta);
        } catch (Exception e) {
                    log.warn("[TaskProgress][analytics] FAILED publish roleId={} taskKey={} delta={} source={} err={}",
                        roleId, taskKey, delta, source, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wallet Events (tracking only — no task key)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "wallet.transaction", groupId = "analytics-service-group")
    public void consumeWalletTransaction(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long playerId = ((Number) event.get("userId")).longValue();
            String type = (String) event.get("type");
            String eventType = type.equals("SPEND") ? "wallet.spent" : "wallet.earned";
            analyticsService.trackEvent(playerId, eventType, "economy", event, null);

            // → task-service: spend_gold progress (SPEND transactions only)
            if ("SPEND".equals(type)) {
                Number amountObj = (Number) event.getOrDefault("amount",
                        event.getOrDefault("delta", event.getOrDefault("value", 1)));
                int delta = (amountObj != null) ? amountObj.intValue() : 1;
                publishTaskProgress(playerId, "spend_gold", delta, "wallet");
            }

            ack.acknowledge();
            log.debug("Processed wallet.transaction for player {}", playerId);
        } catch (Exception e) {
            log.error("Failed to process wallet.transaction event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shop Events (tracking only)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "shop.purchase", groupId = "analytics-service-group")
    public void consumeShopPurchase(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long playerId = ((Number) event.get("roleId")).longValue();
            analyticsService.trackEvent(playerId, "shop.purchase", "economy", event, null);
            ack.acknowledge();
            log.debug("Processed shop.purchase for player {}", playerId);
        } catch (Exception e) {
            log.error("Failed to process shop.purchase event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Battle Events — raw tracking (different topic from combat.result)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = {"battle.started", "battle.ended"}, groupId = "analytics-service-group")
    public void consumeBattleEvent(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> participants = (List<Map<String, Object>>) event.get("participants");
            if (participants != null) {
                for (Map<String, Object> participant : participants) {
                    Long playerId = ((Number) participant.get("roleId")).longValue();
                    String eventType = event.containsKey("winner") ? "battle.ended" : "battle.started";
                    analyticsService.trackEvent(playerId, eventType, "combat", event, null);
                }
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process battle event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Combat Result — typed, publishes kill_monster TaskProgressEvent
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "combat.result", groupId = "analytics-service-group")
    public void consumeCombatResult(String message, Acknowledgment ack) {
        try {
            CombatResultEvent event = objectMapper.readValue(message, CombatResultEvent.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            analyticsService.trackEvent(event.getRoleId(), "combat.result", "combat", payload, null);

            // → task-service: kill_monster progress
            if (event.getKillCount() != null && event.getKillCount() > 0) {
                publishTaskProgress(event.getRoleId(), "kill_monster", event.getKillCount(), "combat");
            }

            ack.acknowledge();
            log.debug("Processed combat.result: roleId={}, kills={}", event.getRoleId(), event.getKillCount());
        } catch (Exception e) {
            log.error("Failed to process combat.result event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Arena Events — raw tracking (arena.match.started / arena.match.ended)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = {"arena.match.started", "arena.match.ended"}, groupId = "analytics-service-group")
    public void consumeArenaEvent(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long playerId = ((Number) event.get("roleId")).longValue();
            analyticsService.trackEvent(playerId, "arena.match", "combat", event, null);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process arena event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Arena Match End — typed, publishes arena_win TaskProgressEvent (winner only)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "arena.match.end", groupId = "analytics-service-group")
    public void consumeArenaMatchEnd(String message, Acknowledgment ack) {
        try {
            ArenaMatchEndEvent event = objectMapper.readValue(message, ArenaMatchEndEvent.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            analyticsService.trackEvent(event.getWinnerId(), "arena.match.end", "combat", payload, null);

            // → task-service: arena_win progress (winner only)
            publishTaskProgress(event.getWinnerId(), "arena_win", 1, "arena-service");

            ack.acknowledge();
            log.debug("Processed arena.match.end: winnerId={}, loserId={}", event.getWinnerId(), event.getLoserId());
        } catch (Exception e) {
            log.error("Failed to process arena.match.end event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Role/Level Events (tracking only)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "role.level.up", groupId = "analytics-service-group")
    public void consumeRoleLevelUp(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long playerId = ((Number) event.get("roleId")).longValue();
            analyticsService.trackEvent(playerId, "role.levelup", "progression", event, null);

            // → task-service: level_up progress — send absolute newLevel so SNAPSHOT_MAX works correctly.
            // task-service uses max(currentProgress, delta), so repeated events are idempotent.
            Number newLevel = (Number) event.get("newLevel");
            if (newLevel != null && newLevel.intValue() > 0) {
                publishTaskProgress(playerId, "level_up", newLevel.intValue(), "role-service");
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process role.level.up event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player Login Events — daily_login task progress
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "player.login", groupId = "analytics-service-group")
    public void consumePlayerLogin(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long roleId = ((Number) event.get("roleId")).longValue();
            analyticsService.trackEvent(roleId, "player.login", "auth", event, null);

            // → task-service: daily_login progress (delta=1 per login, task-service deduplicates via CLAIMED status)
            publishTaskProgress(roleId, "daily_login", 1, "websocket-server");

            ack.acknowledge();
            log.debug("Processed player.login: roleId={}", roleId);
        } catch (Exception e) {
            log.error("Failed to process player.login event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trial Completed — typed, publishes trial_complete TaskProgressEvent
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "trial.completed", groupId = "analytics-service-group")
    public void consumeTrialCompleted(String message, Acknowledgment ack) {
        try {
            TrialCompletedEvent event = objectMapper.readValue(message, TrialCompletedEvent.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            analyticsService.trackEvent(event.getRoleId(), "trial.completed", "progression", payload, null);

            // → task-service: trial_complete progress
            publishTaskProgress(event.getRoleId(), "trial_complete", 1, "trial-service");

            ack.acknowledge();
            log.debug("Processed trial.completed: roleId={}, trialId={}", event.getRoleId(), event.getTrialId());
        } catch (Exception e) {
            log.error("Failed to process trial.completed event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task Completed (tracking only — internal loop, no re-publish)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "task.completed", groupId = "analytics-service-group")
    public void consumeTaskCompleted(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long playerId = ((Number) event.get("roleId")).longValue();
            analyticsService.trackEvent(playerId, "task.completed", "progression", event, null);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process task.completed event", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Guild Events — typed, publishes create_guild / join_guild TaskProgressEvent
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = "guild.created", groupId = "analytics-service-group")
    public void consumeGuildCreated(String message, Acknowledgment ack) {
        try {
            GuildCreatedEvent event = objectMapper.readValue(message, GuildCreatedEvent.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            analyticsService.trackEvent(event.getRoleId(), "guild.created", "social", payload, null);

            // → task-service: create_guild progress
            publishTaskProgress(event.getRoleId(), "create_guild", 1, "guild-service");

            ack.acknowledge();
            log.debug("Processed guild.created: guildId={}, roleId={}", event.getGuildId(), event.getRoleId());
        } catch (Exception e) {
            log.error("Failed to process guild.created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "guild.joined", groupId = "analytics-service-group")
    public void consumeGuildJoined(String message, Acknowledgment ack) {
        try {
            GuildJoinedEvent event = objectMapper.readValue(message, GuildJoinedEvent.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            analyticsService.trackEvent(event.getRoleId(), "guild.joined", "social", payload, null);

            // → task-service: join_guild progress
            publishTaskProgress(event.getRoleId(), "join_guild", 1, "guild-service");

            ack.acknowledge();
            log.debug("Processed guild.joined: guildId={}, roleId={}", event.getGuildId(), event.getRoleId());
        } catch (Exception e) {
            log.error("Failed to process guild.joined event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "guild.left", groupId = "analytics-service-group")
    public void consumeGuildLeft(String message, Acknowledgment ack) {
        try {
            GuildLeftEvent event = objectMapper.readValue(message, GuildLeftEvent.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);
            // Tracking only — no TaskProgressEvent
            analyticsService.trackEvent(event.getRoleId(), "guild.left", "social", payload, null);
            ack.acknowledge();
            log.debug("Processed guild.left: guildId={}, roleId={}, reason={}", event.getGuildId(), event.getRoleId(), event.getReason());
        } catch (Exception e) {
            log.error("Failed to process guild.left event: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pet Events (tracking only)
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = {"pet.activated", "pet.level.up", "pet.evolved"}, groupId = "analytics-service-group")
    public void consumePetEvent(String message, Acknowledgment ack) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long playerId = ((Number) event.get("roleId")).longValue();
            analyticsService.trackEvent(playerId, "pet.action", "progression", event, null);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process pet event", e);
        }
    }
}
