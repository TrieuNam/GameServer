package com.SouthMillion.rune_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RuneEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String RUNE_CREATED_TOPIC = "rune.created";
    private static final String RUNE_EQUIPPED_TOPIC = "rune.equipped";
    
    public void publishRuneCreated(Long userId, Integer runeId, Integer runeIndex, Integer quality) {
        try {
            RuneCreatedEvent event = new RuneCreatedEvent(userId, runeId, runeIndex, quality);
            kafkaTemplate.send(RUNE_CREATED_TOPIC, userId.toString(), 
                objectMapper.writeValueAsString(event));
            log.info("Published rune created event: userId={}, runeId={}", userId, runeId);
        } catch (JsonProcessingException e) {
            log.error("Error publishing rune created event", e);
        }
    }
    
    public void publishRuneEquipped(Long userId, Integer runeId, Integer runeIndex, 
                                   Integer equipSlot, Long combatPower) {
        try {
            RuneEquippedEvent event = new RuneEquippedEvent(
                userId, runeId, runeIndex, equipSlot, combatPower);
            kafkaTemplate.send(RUNE_EQUIPPED_TOPIC, userId.toString(), 
                objectMapper.writeValueAsString(event));
            log.info("Published rune equipped event: userId={}, runeId={}, slot={}", 
                userId, runeId, equipSlot);
        } catch (JsonProcessingException e) {
            log.error("Error publishing rune equipped event", e);
        }
    }
}
