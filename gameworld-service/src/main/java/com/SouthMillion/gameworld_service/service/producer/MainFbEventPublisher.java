package com.SouthMillion.gameworld_service.service.producer;

import org.SouthMillion.dto.gameworld.MainFbEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
public class MainFbEventPublisher {
    @Autowired
    private KafkaTemplate<String, MainFbEvent> kafka;

    public void publish(MainFbEvent event) {
        kafka.send("mainfb-event", event);
    }
}
