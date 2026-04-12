package com.SouthMillion.webSocket_server.handler.rune;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.RuneFeign;
import com.SouthMillion.webSocket_server.service.grpc.RuneGrpcClient;
import org.SouthMillion.proto.Msgrune.Msgrune;
import org.SouthMillion.proto.rune.EquipRuneResponse;
import org.SouthMillion.proto.rune.GetAllRunesResponse;
import org.SouthMillion.proto.rune.GetEquippedRunesResponse;
import org.SouthMillion.proto.rune.UnequipRuneResponse;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class RuneHandlerTest {

    @Mock
    private RuneGrpcClient runeGrpcClient;
    @Mock
    private RuneFeign runeFeign;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private RuneHandler runeHandler;

    @Test
    void handleWearRune_shouldMapClientParamsToGrpc() {
        PlayerSession session = mock(PlayerSession.class);
        Sinks.Many<byte[]> outbound = mock(Sinks.Many.class);
        lenient().when(session.getRoleId()).thenReturn(2001L);
        lenient().when(session.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);

        when(runeGrpcClient.equipRune("2001", 123, 4)).thenReturn(
            EquipRuneResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setSuccess(true).build())
                .build());
        when(runeGrpcClient.getAllRunes("2001")).thenReturn(GetAllRunesResponse.newBuilder().build());

        Msgrune.PB_CSRuneReq req = Msgrune.PB_CSRuneReq.newBuilder()
            .setOperType(3)
            .setP1(4)
            .setP2(123)
            .build();

        runeHandler.handle(session, 1671, req.toByteArray()).block();

        verify(runeGrpcClient).equipRune("2001", 123, 4);
    }

    @Test
    void handleOffRune_shouldCallUnequipBySlot() {
        PlayerSession session = mock(PlayerSession.class);
        Sinks.Many<byte[]> outbound = mock(Sinks.Many.class);
        lenient().when(session.getRoleId()).thenReturn(2001L);
        lenient().when(session.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);

        when(runeGrpcClient.offRune("2001", 7)).thenReturn(
            UnequipRuneResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setCode(200).setSuccess(true).build())
                .build());
        when(runeGrpcClient.getAllRunes("2001")).thenReturn(GetAllRunesResponse.newBuilder().build());

        Msgrune.PB_CSRuneReq req = Msgrune.PB_CSRuneReq.newBuilder()
            .setOperType(4)
            .setP1(7)
            .build();

        runeHandler.handle(session, 1671, req.toByteArray()).block();

        verify(runeGrpcClient).offRune("2001", 7);
    }

    @Test
    void handleRuneBoxDraw_shouldRefreshInfoAndEmitNotice() {
        PlayerSession session = mock(PlayerSession.class);
        Sinks.Many<byte[]> outbound = mock(Sinks.Many.class);
        lenient().when(session.getRoleId()).thenReturn(2001L);
        lenient().when(session.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);
        when(runeFeign.drawBox(2001L, 2)).thenReturn(org.springframework.http.ResponseEntity.ok(Map.of(
            "success", true,
            "rewards", List.of(Map.of("itemId", 70000, "num", 1))
        )));
        when(runeGrpcClient.getAllRunes("2001")).thenReturn(GetAllRunesResponse.newBuilder().build());
        when(runeGrpcClient.getEquippedRunes("2001")).thenReturn(GetEquippedRunesResponse.newBuilder().build());

        Msgrune.PB_CSRuneReq req = Msgrune.PB_CSRuneReq.newBuilder()
            .setOperType(8)
            .setP1(2)
            .build();

        runeHandler.handle(session, 1671, req.toByteArray()).block();

        ArgumentCaptor<byte[]> frames = ArgumentCaptor.forClass(byte[].class);
        verify(runeFeign).drawBox(2001L, 2);
        verify(outbound, org.mockito.Mockito.atLeastOnce()).tryEmitNext(frames.capture());

        List<Integer> emittedMsgIds = new ArrayList<>();
        for (byte[] frame : frames.getAllValues()) {
            var decoded = com.SouthMillion.webSocket_server.net.PacketCodec.decode(frame);
            if (decoded != null) {
                emittedMsgIds.add(decoded.msgId());
            }
        }
        assertTrue(emittedMsgIds.contains(MessageIds.SC_GET_ITEM_NOTICE));
        assertTrue(emittedMsgIds.contains(1670));
    }

    @Test
    void handleRuneBoxDraw_shouldEmitRetWhenFailed() {
        PlayerSession session = mock(PlayerSession.class);
        Sinks.Many<byte[]> outbound = mock(Sinks.Many.class);
        lenient().when(session.getRoleId()).thenReturn(2001L);
        lenient().when(session.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);
        when(runeFeign.drawBox(2001L, 2)).thenReturn(org.springframework.http.ResponseEntity.ok(Map.of("success", false)));

        Msgrune.PB_CSRuneReq req = Msgrune.PB_CSRuneReq.newBuilder()
                .setOperType(8)
                .setP1(2)
                .build();

        runeHandler.handle(session, 1671, req.toByteArray()).block();

        ArgumentCaptor<byte[]> frames = ArgumentCaptor.forClass(byte[].class);
        verify(runeFeign).drawBox(2001L, 2);
        verify(outbound, org.mockito.Mockito.atLeastOnce()).tryEmitNext(frames.capture());

        List<Integer> emittedMsgIds = new ArrayList<>();
        for (byte[] frame : frames.getAllValues()) {
            var decoded = com.SouthMillion.webSocket_server.net.PacketCodec.decode(frame);
            if (decoded != null) {
                emittedMsgIds.add(decoded.msgId());
            }
        }
        assertTrue(emittedMsgIds.contains(1672));
        assertFalse(emittedMsgIds.contains(MessageIds.SC_GET_ITEM_NOTICE));
    }

    @Test
    void handle_shouldSkipWhenRoleIdNull() {
        PlayerSession session = mock(PlayerSession.class);
        Sinks.Many<byte[]> outbound = mock(Sinks.Many.class);
        lenient().when(session.getRoleId()).thenReturn(null);
        lenient().when(session.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);

        Msgrune.PB_CSRuneReq req = Msgrune.PB_CSRuneReq.newBuilder()
                .setOperType(8)
                .setP1(2)
                .build();

        runeHandler.handle(session, 1671, req.toByteArray()).block();

        verifyNoInteractions(runeFeign);
        verify(outbound, org.mockito.Mockito.atLeastOnce()).tryEmitNext(any(byte[].class));
    }

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(runeHandler, "publishTaskProgress", 2001L, "condition_85", "websocket-rune-level-up");

        verify(taskProgressPublisher).publish(2001L, "condition_85", 1, "websocket-rune-level-up");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(runeHandler, "publishTaskProgress", 2001L, "", "websocket-rune-level-up");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
