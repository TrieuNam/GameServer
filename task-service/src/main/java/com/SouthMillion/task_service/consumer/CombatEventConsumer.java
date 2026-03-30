package com.SouthMillion.task_service.consumer;

import com.SouthMillion.task_service.service.AchievementService;
import com.SouthMillion.task_service.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.combat.CombatResultEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes combat events for achievement tracking and statistics
 * Listens to: combat.result
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CombatEventConsumer {

    private final AchievementService achievementService;
    private final StatisticsService statisticsService;

    /**
     * Handle combat result events
     * - Update combat statistics (kills, deaths, damage, healing)
     * - Track combat achievements (combo, kill count, damage milestones)
     * - Update quest progress (kill X enemies)
     */
    @KafkaListener(topics = "combat.result", groupId = "task-service")
    public void handleCombatResult(
            @Payload CombatResultEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        try {
            log.info("Received combat result event: eventId={}, roleId={}, type={}, victory={}, kills={}", 
                    event.getEventId(), event.getRoleId(), event.getCombatType(), 
                    event.getIsVictory(), event.getKillCount());

            // Update combat achievements
            achievementService.checkCombatAchievements(
                    String.valueOf(event.getRoleId()),
                    event.getIsVictory(),
                    event.getKillCount(),
                    event.getComboMax(),
                    event.getTotalDamage()
            );
            
            // Update combat statistics
            statisticsService.updateCombatStats(
                    String.valueOf(event.getRoleId()),
                    event.getIsVictory(),
                    event.getKillCount(),
                    event.getDeathCount(),
                    event.getTotalDamage(),
                    event.getTotalHealing(),
                    event.getComboMax()
            );
            
            // Task progress (kill_monster) is now handled by analytics-service
            // via task.progress.update topic → TaskProgressEventConsumer

            log.debug("Successfully processed combat result event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing combat result event: {}", event.getEventId(), e);
        }
    }
}
