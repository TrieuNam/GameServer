package com.SouthMillion.webSocket_server.handler.box;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.handler.task.TaskHandler;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BoxFeign;
import com.SouthMillion.webSocket_server.service.client.EquipHttpClient;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoxHandlerTest {

    @Mock private BoxFeign boxFeign;
    @Mock private EquipHttpClient equipHttpClient;
    @Mock private RoleServiceHandler roleServiceHandler;
    @Mock private RoleFeign roleFeign;
    @Mock private TaskProgressPublisher taskProgressPublisher;
    @Mock private WalletHttpClient walletHttpClient;
    @Mock private TaskHandler taskHandler;

    @InjectMocks private BoxHandler boxHandler;

    private PlayerSession playerSession;
    private Sinks.Many<byte[]> outbound;

    @BeforeEach
    void setUp() {
        playerSession = org.mockito.Mockito.mock(PlayerSession.class);
        @SuppressWarnings("unchecked")
        Sinks.Many<byte[]> sink = org.mockito.Mockito.mock(Sinks.Many.class);
        outbound = sink;
        when(playerSession.getUserId()).thenReturn("1001");
        when(playerSession.getRoleId()).thenReturn(2001L);
        when(playerSession.getUsername()).thenReturn("testUser");
        when(playerSession.getOutbound()).thenReturn(outbound);
        Mockito.lenient().when(outbound.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
        Mockito.lenient().when(taskProgressPublisher.publish(any(), any(), any(Integer.class), any())).thenReturn(true);
        Mockito.lenient().when(roleServiceHandler.pushRoleState(any())).thenReturn(Mono.empty());
        Mockito.lenient().when(roleFeign.getOtherRole(any(), any())).thenReturn(null);
        Mockito.lenient().when(boxFeign.info(any())).thenReturn(BoxDTOs.InfoResp.builder()
                .boxLevel(1)
                .boxBuyTimes(0)
                .openBoxTotal(0)
                .levelFetchFlag(0)
                .build());
        Mockito.lenient().when(boxFeign.getSetting(any())).thenReturn(BoxDTOs.BoxSettingResp.builder().build());
        Mockito.lenient().when(boxFeign.equipInfo(any())).thenReturn(BoxDTOs.EquipInfo.builder().build());
    }

    @Test
    @DisplayName("Open box reports progress and forwards open request")
    void handleOpen_reportsTaskProgressAndForwardsOpen() {
        when(boxFeign.open(any())).thenReturn(BoxDTOs.OpenResp.builder().build());

        Msgbox.PB_CSBoxReq req = Msgbox.PB_CSBoxReq.newBuilder()
                .setReqType(1)
                .setParam(0)
                .build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, req.toByteArray()).block());

        verify(taskProgressPublisher).publish(2001L, "condition_3", 1, "websocket-box-open");
        verify(taskProgressPublisher).publish(2001L, "get_equip", 1, "websocket-box-open");
        verify(taskHandler, never()).pushCurrentTaskProgress(playerSession);

        ArgumentCaptor<BoxDTOs.OpenReq> openReqCaptor = ArgumentCaptor.forClass(BoxDTOs.OpenReq.class);
        verify(boxFeign).open(openReqCaptor.capture());
        assertEquals("2001", openReqCaptor.getValue().getRoleId());
        assertEquals(1, openReqCaptor.getValue().getCount());
        assertEquals(1, openReqCaptor.getValue().getRoleLevel());
    }

    @Test
    @DisplayName("Open box pushes immediate task snapshot when publish fails")
    void handleOpen_publishFails_pushesImmediateTaskSnapshot() {
        when(taskProgressPublisher.publish(eq(2001L), eq("condition_3"), eq(1), eq("websocket-box-open")))
                .thenReturn(false);
        when(taskProgressPublisher.publish(eq(2001L), eq("get_equip"), eq(1), eq("websocket-box-open")))
                .thenReturn(false);
        when(boxFeign.open(any())).thenReturn(BoxDTOs.OpenResp.builder().build());

        Msgbox.PB_CSBoxReq req = Msgbox.PB_CSBoxReq.newBuilder()
                .setReqType(1)
                .setParam(0)
                .build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, req.toByteArray()).block());

        verify(taskHandler, times(2)).pushCurrentTaskProgress(playerSession);
    }

    @Test
    @DisplayName("Regression flow calls wear sell decompose and reconnect pushAll")
    void regressionFlow_wearSellDecomposeReconnect() {
        when(boxFeign.wear(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("OK").build());
        when(boxFeign.sell(any())).thenReturn(BoxDTOs.SellResp.builder().ok(true).message("OK").sellCoin(5L).sellExp(1L).build());
        when(boxFeign.decompose(any())).thenReturn(BoxDTOs.DecomposeResp.builder().ok(true).message("Decomposed").gotItemId(77).gotNum(2).gotExp(10).build());

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();
        Msgbox.PB_CSBoxReq sellReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(3).build();
        Msgbox.PB_CSBoxReq decomposeReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(7).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());
        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, sellReq.toByteArray()).block());
        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, decomposeReq.toByteArray()).block());
        assertDoesNotThrow(() -> boxHandler.pushAll(playerSession).block());

        verify(boxFeign).wear(any());
        verify(boxFeign).sell(any());
        verify(boxFeign).decompose(2001L);
        verify(boxFeign, atLeastOnce()).equipInfo(2001L);
        verify(boxFeign, atLeastOnce()).info(2001L);
        verify(boxFeign).getSetting(2001L);
    }

    @Test
    @DisplayName("Wear success clears compare state")
    void handleEquip_success_clearsCompareState() {
        when(boxFeign.wear(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("OK").build());

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        verify(boxFeign).wear(any());
        verify(boxFeign).clearCompareState(2001L);
    }

        @Test
        @DisplayName("Wear success emits 1615 and 1616 packets")
            void handleEquip_success_emitsCloseAndRefreshPackets() {
        when(boxFeign.wear(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("OK").build());

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frameCaptor.capture());

        boolean has1615 = frameCaptor.getAllValues().stream()
            .map(PacketCodec::decode)
            .filter(java.util.Objects::nonNull)
            .anyMatch(decoded -> decoded.msgId() == 1615);

        boolean has1616 = frameCaptor.getAllValues().stream()
            .map(PacketCodec::decode)
            .filter(java.util.Objects::nonNull)
            .anyMatch(decoded -> decoded.msgId() == 1616);

        assertTrue(has1615, "Expected SC 1615 equip info packet");
        assertTrue(has1616, "Expected SC 1616 box info packet");
        }

    @Test
    @DisplayName("Wear failure does not clear compare state")
    void handleEquip_fail_doesNotClearCompareState() {
        when(boxFeign.wear(any())).thenReturn(BoxDTOs.OkResp.builder().ok(false).message("WEAR_FAILED").build());

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        verify(boxFeign).wear(any());
        verify(boxFeign, never()).clearCompareState(2001L);
    }

    @Test
    @DisplayName("Wear failure emits only refresh 1616 (no 1615 close packet)")
    void handleEquip_fail_emitsOnlyRefreshPacket() {
        when(boxFeign.wear(any())).thenReturn(BoxDTOs.OkResp.builder().ok(false).message("WEAR_FAILED").build());

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frameCaptor.capture());

        boolean has1615 = frameCaptor.getAllValues().stream()
                .map(PacketCodec::decode)
                .filter(java.util.Objects::nonNull)
                .anyMatch(decoded -> decoded.msgId() == 1615);

        boolean has1616 = frameCaptor.getAllValues().stream()
                .map(PacketCodec::decode)
                .filter(java.util.Objects::nonNull)
                .anyMatch(decoded -> decoded.msgId() == 1616);

        assertFalse(has1615, "Did not expect SC 1615 when wear failed");
        assertTrue(has1616, "Expected SC 1616 box info packet");
    }

    @Test
    @DisplayName("Wear null response behaves like failure and does not clear compare state")
    void handleEquip_nullResponse_doesNotClearCompareState() {
        when(boxFeign.wear(any())).thenReturn(null);

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        verify(boxFeign).wear(any());
        verify(boxFeign, never()).clearCompareState(2001L);
    }

    @Test
    @DisplayName("Wear exception sends fallback box info and does not clear compare state")
    void handleEquip_wearThrows_sendsFallbackAndNoClear() {
        when(boxFeign.wear(any())).thenThrow(new RuntimeException("wear down"));

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        verify(boxFeign).wear(any());
        verify(boxFeign, never()).clearCompareState(2001L);

        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frameCaptor.capture());
        boolean has1616 = frameCaptor.getAllValues().stream()
                .map(PacketCodec::decode)
                .filter(java.util.Objects::nonNull)
                .anyMatch(decoded -> decoded.msgId() == 1616);
        assertTrue(has1616, "Expected fallback SC 1616 box info packet");
    }

    @Test
    @DisplayName("Wear success with clearCompareState exception still refreshes box info")
    void handleEquip_clearCompareStateThrows_stillRefreshesInfo() {
        when(boxFeign.wear(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("OK").build());
        Mockito.doThrow(new RuntimeException("redis down")).when(boxFeign).clearCompareState(2001L);

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        verify(boxFeign).wear(any());
        verify(boxFeign).clearCompareState(2001L);
        verify(boxFeign, atLeastOnce()).info(2001L);
    }

    @Test
    @DisplayName("Wear success with clearCompareState exception still emits 1615 and 1616")
    void handleEquip_clearCompareStateThrows_stillEmitsCloseAndRefreshPackets() {
        when(boxFeign.wear(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("OK").build());
        Mockito.doThrow(new RuntimeException("redis down")).when(boxFeign).clearCompareState(2001L);

        Msgbox.PB_CSBoxReq wearReq = Msgbox.PB_CSBoxReq.newBuilder().setReqType(2).build();

        assertDoesNotThrow(() -> boxHandler.handle(playerSession, 1610, wearReq.toByteArray()).block());

        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frameCaptor.capture());

        boolean has1615 = frameCaptor.getAllValues().stream()
                .map(PacketCodec::decode)
                .filter(java.util.Objects::nonNull)
                .anyMatch(decoded -> decoded.msgId() == 1615);

        boolean has1616 = frameCaptor.getAllValues().stream()
                .map(PacketCodec::decode)
                .filter(java.util.Objects::nonNull)
                .anyMatch(decoded -> decoded.msgId() == 1616);

        assertTrue(has1615, "Expected SC 1615 equip info packet");
        assertTrue(has1616, "Expected SC 1616 box info packet");
    }
}