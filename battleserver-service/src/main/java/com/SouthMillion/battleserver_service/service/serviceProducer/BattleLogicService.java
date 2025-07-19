package com.SouthMillion.battleserver_service.service.serviceProducer;

import com.SouthMillion.battleserver_service.event.BattleEvent;
import com.SouthMillion.battleserver_service.event.producer.BattleEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BattleLogicService {
    @Autowired
    private BattleEventProducer battleEventProducer;

    public void endBattle(String battleId, String resultData) {
        // Xử lý logic end battle (nếu cần)
        // Gửi event lên Kafka
        BattleEvent event = new BattleEvent();
        event.setBattleId(battleId);
        event.setEventType("END");
        event.setData(resultData);
        event.setTimestamp(System.currentTimeMillis());
        battleEventProducer.sendBattleEvent(event);
    }
}
