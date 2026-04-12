package com.SouthMillion.webSocket_server.handler.pet;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.PetFeign;
import org.SouthMillion.proto.Msgpet.Msgpet;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PetHandler
 */
@ExtendWith(MockitoExtension.class)
class PetHandlerTest {

    @Mock
    private PetFeign petFeign;

    @Mock
    private TaskProgressPublisher taskProgressPublisher;

    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private PetHandler petHandler;

    private PlayerSession playerSession;
    private Sinks.Many<byte[]> outbound;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        outbound = mock(Sinks.Many.class);
        lenient().when(playerSession.getUserId()).thenReturn("1001");
        lenient().when(playerSession.getRoleId()).thenReturn(2001L);
        lenient().when(playerSession.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);
    }

    @Test
    void testInterests() {
        int[] interests = petHandler.interests();
        
        assertNotNull(interests);
        assertEquals(2, interests.length);
        assertEquals(2110, interests[0]);
    }

    @Test
    void testHandleWithNullRoleId() {
        lenient().when(playerSession.getRoleId()).thenReturn(null);
        
        assertDoesNotThrow(() -> 
            petHandler.handle(playerSession, 2110, new byte[0]).block()
        );
    }

    @Test
    void testHandlePetRequest() {
        when(petFeign.getRolePets("2001")).thenReturn(Map.of("pets", List.of()));
        Msgpet.PB_CSRolePetReq req = Msgpet.PB_CSRolePetReq.newBuilder()
                .setReqType(1)
                .build();
        
        assertDoesNotThrow(() -> 
            petHandler.handle(playerSession, 2110, req.toByteArray()).block()
        );
        
        verify(petFeign).getRolePets("2001");
        verify(outbound, atLeastOnce()).tryEmitNext(any(byte[].class));
    }

    @Test
    void testHandleOneKeyUpLevelGem() {
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> 
            petHandler.handle(playerSession, 2105, payload).block()
        );
    }

    @Test
    void publishTaskProgress_shouldPublishOnSuccess() {
        ReflectionTestUtils.invokeMethod(
                petHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", true),
                "condition_51",
                "websocket-pet-activate"
        );

        verify(taskProgressPublisher).publish(2001L, "condition_51", 1, "websocket-pet-activate");
    }

    @Test
    void publishTaskProgress_shouldSkipWhenFailed() {
        ReflectionTestUtils.invokeMethod(
                petHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", false),
                "condition_51",
                "websocket-pet-activate"
        );

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void handleTreasureDraw_shouldEmitRewardNotice() {
        when(petFeign.drawTreasure("2001", 1)).thenReturn(Map.of(
                "success", true,
                "rewards", List.of(Map.of("itemId", 61002, "num", 1), Map.of("itemId", 70105, "num", 1))
        ));

        Msgpet.PB_CSRolePetReq req = Msgpet.PB_CSRolePetReq.newBuilder()
                .setReqType(12)
                .setParam1(1)
                .build();

        petHandler.handle(playerSession, 2110, req.toByteArray()).block();

        ArgumentCaptor<byte[]> frames = ArgumentCaptor.forClass(byte[].class);
        verify(petFeign).drawTreasure("2001", 1);
        verify(outbound, atLeastOnce()).tryEmitNext(frames.capture());

        List<Integer> emittedMsgIds = new ArrayList<>();
        for (byte[] frame : frames.getAllValues()) {
            var decoded = com.SouthMillion.webSocket_server.net.PacketCodec.decode(frame);
            if (decoded != null) {
                emittedMsgIds.add(decoded.msgId());
            }
        }
        assertTrue(emittedMsgIds.contains(MessageIds.SC_GET_ITEM_NOTICE));
    }

    @Test
    void handleTreasureDraw_shouldSendErrorResponseWhenFailed() {
        when(petFeign.drawTreasure("2001", 1)).thenReturn(Map.of("success", false));

        Msgpet.PB_CSRolePetReq req = Msgpet.PB_CSRolePetReq.newBuilder()
                .setReqType(12)
                .setParam1(1)
                .build();

        petHandler.handle(playerSession, 2110, req.toByteArray()).block();

        ArgumentCaptor<byte[]> frames = ArgumentCaptor.forClass(byte[].class);
        verify(petFeign).drawTreasure("2001", 1);
        verify(outbound, atLeastOnce()).tryEmitNext(frames.capture());

        List<Integer> emittedMsgIds = new ArrayList<>();
        for (byte[] frame : frames.getAllValues()) {
            var decoded = com.SouthMillion.webSocket_server.net.PacketCodec.decode(frame);
            if (decoded != null) {
                emittedMsgIds.add(decoded.msgId());
            }
        }
        assertTrue(emittedMsgIds.contains(2101));
        assertFalse(emittedMsgIds.contains(MessageIds.SC_GET_ITEM_NOTICE));
    }
}
