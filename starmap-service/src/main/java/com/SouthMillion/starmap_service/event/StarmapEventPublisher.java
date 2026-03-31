package com.SouthMillion.starmap_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class StarmapEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String STAR_ACTIVATED_TOPIC = "star.activated";
    private static final String CONSTELLATION_COMPLETED_TOPIC = "constellation.completed";
    
    public void publishStarActivated(Long userId, Integer starId, Integer constellationId, Integer starLevel) {
        try {
            StarActivatedEvent event = new StarActivatedEvent(userId, starId, constellationId, starLevel);
            String eventJson = objectMapper.writeValueAsString(event);
            String key = userId.toString();
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(STAR_ACTIVATED_TOPIC, key, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published star activated event: userId={}, starId={}, eventId={}", 
                        userId, starId, event.getEventId());
                } else {
                    log.error("Failed to publish star activated event: userId={}, starId={}", 
                        userId, starId, ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing star activated event", e);
        }
    }
    
    public void publishConstellationCompleted(Long userId, Integer constellationId, 
                                             Integer totalStars, Integer level, Long power) {
        try {
            ConstellationCompletedEvent event = new ConstellationCompletedEvent(
                userId, constellationId, totalStars, level, power);
            String eventJson = objectMapper.writeValueAsString(event);
            String key = userId.toString();
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(CONSTELLATION_COMPLETED_TOPIC, key, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published constellation completed event: userId={}, constellationId={}, eventId={}", 
                        userId, constellationId, event.getEventId());
                } else {
                    log.error("Failed to publish constellation completed event: userId={}, constellationId={}", 
                        userId, constellationId, ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing constellation completed event", e);
        }
    }
}
