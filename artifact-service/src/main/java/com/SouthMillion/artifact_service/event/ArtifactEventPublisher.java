package com.SouthMillion.artifact_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArtifactEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String ARTIFACT_UNLOCKED_TOPIC = "artifact.unlocked";
    private static final String ARTIFACT_AWAKENED_TOPIC = "artifact.awakened";
    
    public void publishArtifactUnlocked(Long userId, Integer artifactId, Integer artifactIndex) {
        try {
            ArtifactUnlockedEvent event = new ArtifactUnlockedEvent(userId, artifactId, artifactIndex);
            kafkaTemplate.send(ARTIFACT_UNLOCKED_TOPIC, userId.toString(), 
                objectMapper.writeValueAsString(event));
            log.info("Published artifact unlocked event: userId={}, artifactId={}", userId, artifactId);
        } catch (JsonProcessingException e) {
            log.error("Error publishing artifact unlocked event", e);
        }
    }
    
    public void publishArtifactAwakened(Long userId, Integer artifactId, Integer artifactIndex, 
                                       Integer oldStage, Integer newStage) {
        try {
            ArtifactAwakenedEvent event = new ArtifactAwakenedEvent(
                userId, artifactId, artifactIndex, oldStage, newStage);
            kafkaTemplate.send(ARTIFACT_AWAKENED_TOPIC, userId.toString(), 
                objectMapper.writeValueAsString(event));
            log.info("Published artifact awakened event: userId={}, artifactId={}, stage: {} -> {}", 
                userId, artifactId, oldStage, newStage);
        } catch (JsonProcessingException e) {
            log.error("Error publishing artifact awakened event", e);
        }
    }
}
