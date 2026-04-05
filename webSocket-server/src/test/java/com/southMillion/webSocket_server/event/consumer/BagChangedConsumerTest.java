package com.SouthMillion.webSocket_server.event.consumer;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.event.consumer.BagChangedConsumer;
import com.SouthMillion.webSocket_server.service.BagUpdateGate;
import com.SouthMillion.webSocket_server.service.PlayerSessionRegistry;
import org.SouthMillion.dto.event.bag.BagChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BagChangedConsumerTest {

    @Mock
    private PlayerSessionRegistry sessions;

    private BagUpdateGate bagUpdateGate;
    private BagChangedConsumer consumer;
    private PlayerSession playerSession;
    private Sinks.Many<byte[]> outbound;

    @BeforeEach
    void setUp() {
        bagUpdateGate = new BagUpdateGate();
        consumer = new BagChangedConsumer(sessions, bagUpdateGate);

        playerSession = mock(PlayerSession.class);
        @SuppressWarnings("unchecked")
        Sinks.Many<byte[]> sink = mock(Sinks.Many.class);
        outbound = sink;

        lenient().when(playerSession.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
        lenient().when(sessions.sessionsOfRole(2001L)).thenReturn(List.of(playerSession));
    }

    @Test
    @DisplayName("Bag changed event is skipped when the first UI bag push in the flow already succeeded")
    void onChanged_skipsDuplicateSecondUiUpdate() {
        bagUpdateGate.arm(2001L);
        bagUpdateGate.markDelivered(2001L);

        consumer.onChanged(BagChangedEvent.builder()
                .eventId("evt-1")
                .roleId("2001")
                .itemId(40004)
                .delta(1L)
                .newNum(9L)
                .reason("grant")
                .at(Instant.now())
                .build());

        verify(outbound, never()).tryEmitNext(any());
    }

    @Test
    @DisplayName("Bag changed event still reaches the UI when the first push failed and no bag snapshot was delivered")
    void onChanged_allowsFallbackUiUpdateAfterFirstPushFailure() {
        bagUpdateGate.arm(2001L);

        consumer.onChanged(BagChangedEvent.builder()
                .eventId("evt-2")
                .roleId("2001")
                .itemId(40004)
                .delta(1L)
                .newNum(9L)
                .reason("grant")
                .at(Instant.now())
                .build());

        verify(outbound, times(1)).tryEmitNext(any());
    }
}
