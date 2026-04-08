package com.SouthMillion.webSocket_server.handler.equip;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.equip.EquipHandler;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.EquipHttpClient;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EquipHandlerTest {

    @Mock
    private EquipHttpClient equipHttpClient;

    @Mock
    private TaskProgressPublisher taskProgressPublisher;

    private EquipHandler equipHandler;
    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        equipHandler = new EquipHandler(equipHttpClient, taskProgressPublisher, Schedulers.immediate());
        playerSession = PlayerSession.builder()
                .roleId(2001L)
                .username("testUser")
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
    }

    @Test
    @DisplayName("bag_wearing quality=2 (Lục) publishes condition_19 progress")
    void handleBagWearing_greenQualityPublishesCondition19() {
        when(equipHttpClient.wearableItems("2001")).thenReturn(Map.of(
                "items", List.of(Map.of(
                        "itemId", 40001,
                        "equipType", 1,
                        "quality", 2,
                        "num", 1
                ))
        ));
        when(equipHttpClient.wear("2001", 40001)).thenReturn(null);
        when(equipHttpClient.list("2001")).thenReturn(null);

        Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.newBuilder()
                .setReqType(1)
                .setParam1(0)
                .build();

        assertDoesNotThrow(() -> equipHandler.handle(playerSession, 1600, req.toByteArray()).block());

        verify(taskProgressPublisher).publish(2001L, "get_equip", 1, "websocket-equip");
        verify(taskProgressPublisher).publish(2001L, "condition_19", 1, "websocket-equip");
    }

    @Test
    @DisplayName("bag_wearing quality=4 (Tím) publishes all configured threshold conditions")
    void handleBagWearing_purpleQualityPublishesAllThresholdConditions() {
        when(equipHttpClient.wearableItems("2001")).thenReturn(Map.of(
                "items", List.of(Map.of(
                        "itemId", 40002,
                        "equipType", 1,
                        "quality", 4,
                        "num", 1
                ))
        ));
        when(equipHttpClient.wear("2001", 40002)).thenReturn(null);
        when(equipHttpClient.list("2001")).thenReturn(null);

        Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.newBuilder()
                .setReqType(1)
                .setParam1(0)
                .build();

        assertDoesNotThrow(() -> equipHandler.handle(playerSession, 1600, req.toByteArray()).block());

        verify(taskProgressPublisher).publish(2001L, "condition_19", 1, "websocket-equip");
        verify(taskProgressPublisher).publish(2001L, "condition_20", 1, "websocket-equip");
        verify(taskProgressPublisher).publish(2001L, "condition_21", 1, "websocket-equip");
    }
}
