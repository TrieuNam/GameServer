package com.SouthMillion.trial_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.trial.TrialCompletedEvent;
import org.SouthMillion.dto.event.trial.TrialFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Event Publisher for Trial Service
 * Publishes events to event-driven architecture
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_TRIAL_COMPLETED = "trial.completed";
    private static final String TOPIC_TRIAL_FAILED = "trial.failed";

    /**
     * Publish trial completed event
     */
    public void publishTrialCompleted(Long userId, Long roleId, Integer trialId, 
                                     Integer stageReached, Long score, Integer stars,
                                     Integer completionTime, Boolean isNewRecord, Integer attempts) {
        try {
            TrialCompletedEvent event = TrialCompletedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId(userId)
                    .roleId(roleId)
                    .trialId(trialId)
                    .stageReached(stageReached)
                    .score(score)
                    .stars(stars)
                    .completionTime(completionTime)
                    .isNewRecord(isNewRecord)
                    .attempts(attempts)
                    .source("NORMAL")
                    .completedAt(Instant.now())
                    .build();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    TOPIC_TRIAL_COMPLETED, 
                    roleId.toString(), 
                    event
            );
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[kafka-trial] Published TrialCompletedEvent: roleId={}, trialId={}, score={}", 
                            roleId, trialId, score);
                } else {
                    log.error("[kafka-trial] Failed to publish TrialCompletedEvent: {}", ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("[kafka-trial] Error publishing TrialCompletedEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish trial failed event
     */
    public void publishTrialFailed(Long userId, Long roleId, Integer trialId,
                                   Integer stageReached, String failReason, Integer attempts) {
        try {
            TrialFailedEvent event = TrialFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId(userId)
                    .roleId(roleId)
                    .trialId(trialId)
                    .stageReached(stageReached)
                    .failReason(failReason)
                    .attempts(attempts)
                    .failedAt(Instant.now())
                    .build();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    TOPIC_TRIAL_FAILED,
                    roleId.toString(),
                    event
            );
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[kafka-trial] Published TrialFailedEvent: roleId={}, trialId={}, reason={}", 
                            roleId, trialId, failReason);
                } else {
                    log.error("[kafka-trial] Failed to publish TrialFailedEvent: {}", ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("[kafka-trial] Error publishing TrialFailedEvent: {}", e.getMessage(), e);
        }
    }
}
