package com.SouthMillion.task_service.consumer;

import com.SouthMillion.task_service.service.AchievementService;
import com.SouthMillion.task_service.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.trial.TrialCompletedEvent;
import org.SouthMillion.dto.event.trial.TrialFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes trial events for task/achievement tracking
 * Listens to: trial.completed, trial.failed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrialEventConsumer {

    private final AchievementService achievementService;
    private final StatisticsService statisticsService;

    /**
     * Handle trial completion events
     * - Update task progress (e.g., "Complete Trial X 5 times")
     * - Track achievements (e.g., "First Trial Victory")
     * - Update statistics
     */
    @KafkaListener(topics = "trial.completed", groupId = "task-service")
    public void handleTrialCompleted(
            @Payload TrialCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        try {
            log.info("Received trial completed event: eventId={}, userId={}, roleId={}, trialId={}, stars={}", 
                    event.getEventId(), event.getUserId(), event.getRoleId(), event.getTrialId(), event.getStars());

            // Update achievements
            achievementService.checkTrialAchievements(
                    String.valueOf(event.getRoleId()),
                    event.getTrialId(),
                    event.getStars(),
                    event.getIsNewRecord() != null && event.getIsNewRecord()
            );
            
            // Update statistics
            statisticsService.updateTrialStats(
                    String.valueOf(event.getRoleId()),
                    true, // victory
                    event.getStars(),
                    event.getStageReached(),
                    event.getCompletionTime().longValue()
            );
            
            // Task progress (trial_complete) is now handled by analytics-service
            // via task.progress.update topic → TaskProgressEventConsumer

            log.debug("Successfully processed trial completed event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing trial completed event: {}", event.getEventId(), e);
        }
    }

    /**
     * Handle trial failure events
     * - Track statistics
     * - Update daily attempt counters
     */
    @KafkaListener(topics = "trial.failed", groupId = "task-service")
    public void handleTrialFailed(
            @Payload TrialFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        try {
            log.info("Received trial failed event: eventId={}, userId={}, roleId={}, trialId={}, stage={}", 
                    event.getEventId(), event.getUserId(), event.getRoleId(), event.getTrialId(), event.getStageReached());

            // Update statistics for failed attempt
            statisticsService.updateTrialStats(
                    String.valueOf(event.getRoleId()),
                    false, // not a victory
                    0, // no stars
                    event.getStageReached(),
                    0L // no completion time
            );

            log.debug("Successfully processed trial failed event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing trial failed event: {}", event.getEventId(), e);
        }
    }
}
