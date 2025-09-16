package com.southMillion.webSocket_server.event.consumer;


import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.service.PlayerSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.event.bag.BagChangedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BagChangedConsumer {

    private final PlayerSessionRegistry sessions;

    @KafkaListener(
            topics = "${app.kafka.bag-changed-topic:gameh5.bag.changed}",
            groupId = "${spring.application.name:websocket-service}",
            concurrency = "${app.kafka.concurrency:3}"
    )
    public void onChanged(org.SouthMillion.dto.event.bag.BagChangedEvent ev) {
        if (ev == null || ev.getRoleId() == null || ev.getNewNum() == null) return;
        var list = sessions.sessionsOfRole(ev.getRoleId());
        for (var ps : list) {
            Emitters.sendKnapsackSingleInfo(ps, ev.getItemId(), ev.getNewNum());
        }
    }
}