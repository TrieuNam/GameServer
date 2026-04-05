package com.SouthMillion.webSocket_server.handler.activity;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
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
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenServerActivityHandlerTest {

    @Mock private ActivityFeign activityFeign;

    @InjectMocks private OpenServerActivityHandler openServerActivityHandler;

    private PlayerSession playerSession;
    private Sinks.Many<byte[]> outbound;

    @BeforeEach
    void setUp() {
        playerSession = Mockito.mock(PlayerSession.class);
        @SuppressWarnings("unchecked")
        Sinks.Many<byte[]> sink = Mockito.mock(Sinks.Many.class);
        outbound = sink;

        when(playerSession.getRoleId()).thenReturn(2001L);
        when(playerSession.getOutbound()).thenReturn(outbound);
        Mockito.lenient().when(outbound.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
    }

    @Test
    @DisplayName("pushAll emits open-server activity snapshots during login bootstrap")
    void pushAll_emitsOpenServerActivitySnapshots() {
        when(activityFeign.getSevenDay("2001")).thenReturn(Map.of(
                "endTimestamp", 111,
                "days", 3,
                "receiveFlag", 1
        ));
        when(activityFeign.getLuck("2001")).thenReturn(Map.of(
                "endTimestamp", 222,
                "receiveFlag", 2,
                "openBoxNum", 10,
                "boxLevel", 5
        ));
        when(activityFeign.getNewArea("2001")).thenReturn(Map.of(
                "endTimestamp", 333
        ));
        when(activityFeign.getMarket("2001")).thenReturn(Map.of(
                "endTimestamp", 444,
                "nextFreeRefresh", 555,
                "nextAutoRefresh", 666,
                "curShopGroup", 2,
                "randomCnts", 7
        ));

        assertDoesNotThrow(() -> openServerActivityHandler.pushAll(playerSession).block());

        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frameCaptor.capture());

        Set<Integer> msgIds = frameCaptor.getAllValues().stream()
                .map(PacketCodec::decode)
                .filter(Objects::nonNull)
                .map(decoded -> decoded.msgId())
                .collect(Collectors.toSet());

        assertTrue(msgIds.contains(2161), "Expected 2161 seven-day snapshot packet");
        assertTrue(msgIds.contains(2163), "Expected 2163 luck snapshot packet");
        assertTrue(msgIds.contains(2165), "Expected 2165 new-area snapshot packet");
        assertTrue(msgIds.contains(2167), "Expected 2167 market snapshot packet");
    }
}
