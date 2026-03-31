package com.SouthMillion.angel_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publisher for angel-related events to Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AngelEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String ANGEL_UNLOCKED_TOPIC = "angel.unlocked";
    private static final String ANGEL_LEVEL_UP_TOPIC = "angel.level.up";
    private static final String ANGEL_EVOLVED_TOPIC = "angel.evolved";
    
    /**
     * Publish angel unlocked event
     */
    public void publishAngelUnlocked(Long userId, Integer angelId, Integer angelIndex, 
                                     Integer initialLevel, Integer initialGrade) {
        try {
            AngelUnlockedEvent event = new AngelUnlockedEvent(
                userId, angelId, angelIndex, initialLevel, initialGrade
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            String key = userId.toString();
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(ANGEL_UNLOCKED_TOPIC, key, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published angel unlocked event: userId={}, angelId={}, eventId={}", 
                        userId, angelId, event.getEventId());
                } else {
                    log.error("Failed to publish angel unlocked event: userId={}, angelId={}", 
                        userId, angelId, ex);
                }
            });
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing angel unlocked event", e);
        }
    }
    
    /**
     * Publish angel level up event
     */
    public void publishAngelLevelUp(Long userId, Integer angelId, Integer angelIndex, 
                                    Integer oldLevel, Integer newLevel, Long expGained) {
        try {
            AngelLevelUpEvent event = new AngelLevelUpEvent(
                userId, angelId, angelIndex, oldLevel, newLevel, expGained
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            String key = userId.toString();
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(ANGEL_LEVEL_UP_TOPIC, key, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published angel level up event: userId={}, angelId={}, level: {} -> {}, eventId={}", 
                        userId, angelId, oldLevel, newLevel, event.getEventId());
                } else {
                    log.error("Failed to publish angel level up event: userId={}, angelId={}", 
                        userId, angelId, ex);
                }
            });
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing angel level up event", e);
        }
    }
    
    /**
     * Publish angel evolved event
     */
    public void publishAngelEvolved(Long userId, Integer angelId, Integer angelIndex, 
                                    Integer oldEvolutionStage, Integer newEvolutionStage, 
                                    Integer currentLevel) {
        try {
            AngelEvolvedEvent event = new AngelEvolvedEvent(
                userId, angelId, angelIndex, oldEvolutionStage, newEvolutionStage, currentLevel
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            String key = userId.toString();
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(ANGEL_EVOLVED_TOPIC, key, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published angel evolved event: userId={}, angelId={}, stage: {} -> {}, eventId={}", 
                        userId, angelId, oldEvolutionStage, newEvolutionStage, event.getEventId());
                } else {
                    log.error("Failed to publish angel evolved event: userId={}, angelId={}", 
                        userId, angelId, ex);
                }
            });
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing angel evolved event", e);
        }
    }
}
