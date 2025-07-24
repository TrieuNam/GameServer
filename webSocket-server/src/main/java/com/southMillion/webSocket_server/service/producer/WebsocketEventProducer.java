package com.southMillion.webSocket_server.service.producer;

import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.KafkaEventDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
public class WebsocketEventProducer {
    private final KafkaTemplate<String, KafkaEventDto> kafkaTemplate;
    private static final String TASK_TOPIC = "task-events";
    private static final String REPORT_TOPIC = "report-events";

    public void sendGameEvent(String userId, String type, Object payload) {
        KafkaEventDto event = KafkaEventDto.builder()
                .userId(userId)
                .type(type)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
        kafkaTemplate.send(TASK_TOPIC, event);
        kafkaTemplate.send(REPORT_TOPIC, event);
    }

    public void sendGlobalEvent(String eventType, String data) {
        KafkaEventDto event = KafkaEventDto.builder()
                .type(eventType)
                .payload(data)
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send("global-event-topic", eventType, event);
    }
}

