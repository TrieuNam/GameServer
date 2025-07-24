package com.SouthMillion.item_service.service.producer;

import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.KafkaEventDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemEventProducer {
    private final KafkaTemplate<String, KafkaEventDto> kafkaTemplate;

    public void sendItemEvent(String userId, String type, Object payload) {
        KafkaEventDto event = KafkaEventDto.builder()
                .userId(userId)
                .type(type)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
        kafkaTemplate.send("item-events", userId, event);
    }
}