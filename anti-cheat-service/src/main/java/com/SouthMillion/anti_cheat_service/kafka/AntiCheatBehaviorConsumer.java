package com.SouthMillion.anti_cheat_service.kafka;

import com.SouthMillion.anti_cheat_service.service.AntiCheatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes game-event topics and forwards to AntiCheatService for analysis.
 *
 * Topics:
 *   player-position   — position/movement events → reportMovement
 *   combat-events     — damage/combat results   → reportDamage
 *   resource-events   — resource gain events    → reportResourceGain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AntiCheatBehaviorConsumer {

    private final AntiCheatService antiCheatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Consumes player-position events.
     * Expected JSON payload:
     * { "userId": "...", "x": 0.0, "y": 0.0, "z": 0.0, "speed": 0.0 }
     */
    @KafkaListener(
            topics = "${anticheat.topics.position:player-position}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePositionEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String userId = node.path("userId").asText(null);
            if (userId == null || userId.isBlank()) {
                log.warn("[AntiCheat] consumePositionEvent: missing userId, skipping");
                return;
            }
            double x = node.path("x").asDouble(0.0);
            double y = node.path("y").asDouble(0.0);
            double z = node.path("z").asDouble(0.0);
            double speed = node.path("speed").asDouble(0.0);
            log.debug("[AntiCheat] position event userId={} x={} y={} z={} speed={}", userId, x, y, z, speed);
            antiCheatService.reportMovement(userId, x, y, z, speed);
        } catch (Exception e) {
            log.error("[AntiCheat] consumePositionEvent error: {}", e.getMessage(), e);
        }
    }

    /**
     * Consumes combat-events.
     * Expected JSON payload:
     * { "userId": "...", "targetId": "...", "damage": 0.0, "expectedDamage": 0.0 }
     */
    @KafkaListener(
            topics = "${anticheat.topics.combat:combat-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCombatEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String userId = node.path("userId").asText(null);
            if (userId == null || userId.isBlank()) {
                log.warn("[AntiCheat] consumeCombatEvent: missing userId, skipping");
                return;
            }
            String targetId = node.path("targetId").asText("");
            double damage = node.path("damage").asDouble(0.0);
            double expectedDamage = node.path("expectedDamage").asDouble(0.0);
            log.debug("[AntiCheat] combat event userId={} damage={} expected={}", userId, damage, expectedDamage);
            antiCheatService.reportDamage(userId, targetId, damage, expectedDamage);
        } catch (Exception e) {
            log.error("[AntiCheat] consumeCombatEvent error: {}", e.getMessage(), e);
        }
    }

    /**
     * Consumes resource-events.
     * Expected JSON payload:
     * { "userId": "...", "resourceType": "gold", "amount": 0, "expectedAmount": 0 }
     */
    @KafkaListener(
            topics = "${anticheat.topics.resource:resource-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeResourceEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String userId = node.path("userId").asText(null);
            if (userId == null || userId.isBlank()) {
                log.warn("[AntiCheat] consumeResourceEvent: missing userId, skipping");
                return;
            }
            String resourceType = node.path("resourceType").asText("unknown");
            long amount = node.path("amount").asLong(0L);
            long expectedAmount = node.path("expectedAmount").asLong(0L);
            log.debug("[AntiCheat] resource event userId={} type={} amount={} expected={}", userId, resourceType, amount, expectedAmount);
            antiCheatService.reportResourceGain(userId, resourceType, amount, expectedAmount);
        } catch (Exception e) {
            log.error("[AntiCheat] consumeResourceEvent error: {}", e.getMessage(), e);
        }
    }
}
