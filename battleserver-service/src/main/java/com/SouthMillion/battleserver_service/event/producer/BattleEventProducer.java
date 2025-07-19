package com.SouthMillion.battleserver_service.event.producer;

import com.SouthMillion.battleserver_service.event.BattleEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BattleEventProducer {

    private static final String TOPIC = "battle-event";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendBattleEvent(BattleEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, json);
            System.out.println("Battle event sent: " + json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
