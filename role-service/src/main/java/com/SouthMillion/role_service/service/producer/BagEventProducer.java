package com.SouthMillion.role_service.service.producer;

import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.event.BagGrantEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BagEventProducer {

    private final KafkaTemplate<String, BagGrantEvent> template;

    @Value("${app.kafka.bag-grant-topic:gameh5.bag.grant}")
    private String topic;

    public void publishGrant(String userId, String roleId, List<BagGrantEvent.Item> items, String source) {
        var evt = BagGrantEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .roleId(roleId)
                .source(source)
                .createdAt(Instant.now())
                .items(items)
                .build();
        // partition key = roleId để giữ thứ tự events cùng nhân vật
        template.send(topic, roleId, evt);
    }
}