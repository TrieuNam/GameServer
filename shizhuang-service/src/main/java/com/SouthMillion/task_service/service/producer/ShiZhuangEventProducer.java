package com.SouthMillion.task_service.service.producer;

import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.KafkaEventDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShiZhuangEventProducer {
    private final KafkaTemplate<String, KafkaEventDto> kafkaTemplate;

    public void sendEvent(String userId, String type, Object payload) {
        KafkaEventDto event = KafkaEventDto.builder()
                .userId(userId)
                .type(type)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
        kafkaTemplate.send("shizhuang-events", userId, event);
    }
}
