package com.SouthMillion.webSocket_server.handler.activity;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
import org.SouthMillion.proto.Msgrandactivity.Msgrandactivity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandActivityHandlerTest {

    @Mock
    private ActivityFeign activityFeign;

    @InjectMocks
    private RandActivityHandler randActivityHandler;

    @Test
    void handle_boxFundClientActivityType2049_emitsBoxFundSnapshot() throws Exception {
        PlayerSession session = mock(PlayerSession.class);
        when(session.getRoleId()).thenReturn(2001L);
        when(activityFeign.randActivity(eq("2001"), any(Map.class)))
                .thenReturn(Map.of(
                        "phaseBuyFlag", 1,
                        "commonFetchFlag", 3,
                        "seniorFetchFlag", 7
                ));

        Msgrandactivity.PB_CSRandActivityOperaReq req = Msgrandactivity.PB_CSRandActivityOperaReq.newBuilder()
                .setRandActivityType(2049)
                .setOperaType(0)
                .build();

        AtomicReference<Integer> msgIdRef = new AtomicReference<>();
        AtomicReference<byte[]> payloadRef = new AtomicReference<>();

        try (MockedStatic<Emitters> emitters = mockStatic(Emitters.class)) {
            emitters.when(() -> Emitters.emit(eq(session), anyInt(), any(byte[].class)))
                    .thenAnswer(invocation -> {
                        msgIdRef.set(invocation.getArgument(1));
                        payloadRef.set(invocation.getArgument(2));
                        return null;
                    });

            randActivityHandler.handle(session, 3000, req.toByteArray()).block();

            verify(activityFeign).randActivity(eq("2001"), any(Map.class));
            assertThat(msgIdRef.get()).isEqualTo(3010);
            assertThat(payloadRef.get()).isNotNull();

            Msgrandactivity.PB_SCRaBoxFundInfo info = Msgrandactivity.PB_SCRaBoxFundInfo.parseFrom(payloadRef.get());
            assertThat(info.getPhaseBuyFlag()).isEqualTo(1);
            assertThat(info.getCommonFetchFlag()).isEqualTo(3L);
            assertThat(info.getSeniorFetchFlag()).isEqualTo(7L);
        }
    }

    @Test
    void handle_adEquityClientActivityType2073_emitsAdvertisementEquitySnapshot() throws Exception {
        PlayerSession session = mock(PlayerSession.class);
        when(session.getRoleId()).thenReturn(2001L);
        when(activityFeign.randActivity(eq("2001"), any(Map.class)))
                .thenReturn(Map.of(
                        "isBuy", 1,
                        "fetchFlag", 6,
                        "refreshTime", 123456
                ));

        Msgrandactivity.PB_CSRandActivityOperaReq req = Msgrandactivity.PB_CSRandActivityOperaReq.newBuilder()
                .setRandActivityType(2073)
                .setOperaType(0)
                .build();

        AtomicReference<Integer> msgIdRef = new AtomicReference<>();
        AtomicReference<byte[]> payloadRef = new AtomicReference<>();

        try (MockedStatic<Emitters> emitters = mockStatic(Emitters.class)) {
            emitters.when(() -> Emitters.emit(eq(session), anyInt(), any(byte[].class)))
                    .thenAnswer(invocation -> {
                        msgIdRef.set(invocation.getArgument(1));
                        payloadRef.set(invocation.getArgument(2));
                        return null;
                    });

            randActivityHandler.handle(session, 3000, req.toByteArray()).block();

            verify(activityFeign).randActivity(eq("2001"), any(Map.class));
            assertThat(msgIdRef.get()).isEqualTo(3035);
            assertThat(payloadRef.get()).isNotNull();

            Msgrandactivity.PB_SCRaAdvertisementEquityInfo info = Msgrandactivity.PB_SCRaAdvertisementEquityInfo.parseFrom(payloadRef.get());
            assertThat(info.getIsBuy()).isEqualTo(1);
            assertThat(info.getFetchFlag()).isEqualTo(6);
            assertThat(info.getRefreshTime()).isEqualTo(123456);
        }
    }

    @Test
    void handle_faZhenGalaClientActivityType2063_emitsFestivalSnapshot() throws Exception {
        PlayerSession session = mock(PlayerSession.class);
        when(session.getRoleId()).thenReturn(2001L);
        when(activityFeign.randActivity(eq("2001"), any(Map.class)))
                .thenReturn(Map.of(
                        "level", 18,
                        "endTimestamp", 345678,
                        "fetchFlag", 5,
                        "taskNum", java.util.List.of(18, 18, 18, 18),
                        "giftNum", java.util.List.of(1, 2, 0)
                ));

        Msgrandactivity.PB_CSRandActivityOperaReq req = Msgrandactivity.PB_CSRandActivityOperaReq.newBuilder()
                .setRandActivityType(2063)
                .setOperaType(0)
                .build();

        AtomicReference<Integer> msgIdRef = new AtomicReference<>();
        AtomicReference<byte[]> payloadRef = new AtomicReference<>();

        try (MockedStatic<Emitters> emitters = mockStatic(Emitters.class)) {
            emitters.when(() -> Emitters.emit(eq(session), anyInt(), any(byte[].class)))
                    .thenAnswer(invocation -> {
                        msgIdRef.set(invocation.getArgument(1));
                        payloadRef.set(invocation.getArgument(2));
                        return null;
                    });

            randActivityHandler.handle(session, 3000, req.toByteArray()).block();

            verify(activityFeign).randActivity(eq("2001"), any(Map.class));
            assertThat(msgIdRef.get()).isEqualTo(3024);
            assertThat(payloadRef.get()).isNotNull();

            Msgrandactivity.PB_SCRaFaZhenGalaInfo info = Msgrandactivity.PB_SCRaFaZhenGalaInfo.parseFrom(payloadRef.get());
            assertThat(info.getLevel()).isEqualTo(18);
            assertThat(info.getEndTimestamp()).isEqualTo(345678);
            assertThat(info.getFetchFlag()).isEqualTo(5);
            assertThat(info.getTaskNumList()).containsExactly(18, 18, 18, 18);
            assertThat(info.getGiftNumList()).containsExactly(1, 2, 0);
        }
    }

    @Test
    void handle_affordPresentClientActivityType2067_emitsGiftSnapshot() throws Exception {
        PlayerSession session = mock(PlayerSession.class);
        when(session.getRoleId()).thenReturn(2001L);
        when(activityFeign.randActivity(eq("2001"), any(Map.class)))
                .thenReturn(Map.of(
                        "level", 88,
                        "buyMark", 1,
                        "itemNum", java.util.List.of(1, 0, 0, 0, 0)
                ));

        Msgrandactivity.PB_CSRandActivityOperaReq req = Msgrandactivity.PB_CSRandActivityOperaReq.newBuilder()
                .setRandActivityType(2067)
                .setOperaType(0)
                .build();

        AtomicReference<Integer> msgIdRef = new AtomicReference<>();
        AtomicReference<byte[]> payloadRef = new AtomicReference<>();

        try (MockedStatic<Emitters> emitters = mockStatic(Emitters.class)) {
            emitters.when(() -> Emitters.emit(eq(session), anyInt(), any(byte[].class)))
                    .thenAnswer(invocation -> {
                        msgIdRef.set(invocation.getArgument(1));
                        payloadRef.set(invocation.getArgument(2));
                        return null;
                    });

            randActivityHandler.handle(session, 3000, req.toByteArray()).block();

            verify(activityFeign).randActivity(eq("2001"), any(Map.class));
            assertThat(msgIdRef.get()).isEqualTo(3028);
            assertThat(payloadRef.get()).isNotNull();

            Msgrandactivity.PB_SCRaChaoZhiXianLiInfo info = Msgrandactivity.PB_SCRaChaoZhiXianLiInfo.parseFrom(payloadRef.get());
            assertThat(info.getLevel()).isEqualTo(88);
            assertThat(info.getBuyMark()).isEqualTo(1);
            assertThat(info.getItemNumList()).containsExactly(1, 0, 0, 0, 0);
        }
    }
}
