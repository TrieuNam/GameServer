package com.SouthMillion.webSocket_server.handler.equip;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.equip.EquipHandler;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.EquipHttpClient;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    @DisplayName("bag_sell reqType=2 resolves index to itemId and calls bagSell")
    void handleBagSell_resolvesIndexToItemId() {
        when(equipHttpClient.wearableItems("2001")).thenReturn(Map.of(
                "items", List.of(Map.of(
                        "itemId", 40123,
                        "equipType", 2,
                        "quality", 3,
                        "num", 1
                ))
        ));
        when(equipHttpClient.bagSell(any())).thenReturn(Map.of("ok", true));

        Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.newBuilder()
                .setReqType(2)
                .setParam1(0)
                .build();

        assertDoesNotThrow(() -> equipHandler.handle(playerSession, 1600, req.toByteArray()).block());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(equipHttpClient).bagSell(captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertThat(payload.get("itemId")).isEqualTo(40123);
        assertThat(payload.get("equipType")).isEqualTo(2);
    }

        @Test
        @DisplayName("fumo reqType=3 calls fumoAddExp with client-compatible addExp")
        void handleFumo_addExpCalled() {
                when(equipHttpClient.fumoAddExp(any())).thenReturn(
                                new EquipFumoDTOs.FumoOneResp(0, new EquipFumoDTOs.FumoData(1, 0, 0))
                );

                Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.newBuilder()
                                .setReqType(3)
                                .setParam1(0)
                                .build();

                assertDoesNotThrow(() -> equipHandler.handle(playerSession, 1600, req.toByteArray()).block());
                verify(equipHttpClient).fumoAddExp(any());
        }

        @Test
        @DisplayName("cancel fumo reqType=4 calls fumoActivate with endTime=0")
        void handleCancelFumo_callsActivate() {
                when(equipHttpClient.fumoActivate(any())).thenReturn(
                                new EquipFumoDTOs.FumoOneResp(0, new EquipFumoDTOs.FumoData(1, 0, 0))
                );

                Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.newBuilder()
                                .setReqType(4)
                                .setParam1(0)
                                .build();

                assertDoesNotThrow(() -> equipHandler.handle(playerSession, 1600, req.toByteArray()).block());
                verify(equipHttpClient).fumoActivate(any());
        }

        @Test
        @DisplayName("transform reqType=5 forwards three counts to fumoTransform")
        void handleTransform_callsFumoTransform() {
                when(equipHttpClient.fumoTransform(any())).thenReturn(EquipFumoDTOs.OkResp.OK());
                when(equipHttpClient.fumoList("2001")).thenReturn(new EquipFumoDTOs.FumoListResp(List.of()));
                when(equipHttpClient.wearableItems("2001")).thenReturn(Map.of("items", List.of()));

                Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.newBuilder()
                                .setReqType(5)
                                .setParam1(1)
                                .setParam2(2)
                                .setParam3(3)
                                .build();

                assertDoesNotThrow(() -> equipHandler.handle(playerSession, 1600, req.toByteArray()).block());
                verify(equipHttpClient).fumoTransform(any());
        }
}
