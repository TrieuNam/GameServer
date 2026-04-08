package com.SouthMillion.webSocket_server.handler.mainfb;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.MainFbGrpcClient;
import org.SouthMillion.proto.Msgbattle.Msgbattle;
import org.SouthMillion.proto.Msgmainfb.Msgmainfb;
import org.SouthMillion.proto.mainfb.EnterStageResponse;
import org.SouthMillion.proto.mainfb.GetCurrentTaskResponse;
import org.SouthMillion.proto.mainfb.GetProgressResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainFbHandlerTest {

    @Mock
    private MainFbGrpcClient mainFbGrpcClient;
    @Mock
    private BagFeign bagFeign;
    @Mock
    private WalletHttpClient walletHttpClient;

    @InjectMocks
    private MainFbHandler mainFbHandler;

    @Test
    void handleClaimReward_refreshesBagAndWallet() {
        PlayerSession session = org.mockito.Mockito.mock(PlayerSession.class);
        when(mainFbGrpcClient.getProgress("2001")).thenReturn(GetProgressResponse.newBuilder().build());
        when(bagFeign.list("2001")).thenReturn(List.of());
        when(walletHttpClient.info("2001")).thenReturn(null);

        ReflectionTestUtils.invokeMethod(mainFbHandler, "handleClaimReward", session, 2001L);

        verify(mainFbGrpcClient).claimChapterReward("2001", 1);
        verify(bagFeign, after(400).times(1)).list("2001");
        verify(walletHttpClient, after(400).times(1)).info("2001");
    }

    @Test
    void handleChallenge_startsBattleAndEmitsBattleReport() throws Exception {
        PlayerSession session = org.mockito.Mockito.mock(PlayerSession.class);
        when(session.getRoleId()).thenReturn(2001L);
        when(mainFbGrpcClient.getCurrentTask("2001"))
                .thenReturn(GetCurrentTaskResponse.newBuilder()
                        .setStage(1)
                        .setLevel(1)
                        .setAllDone(false)
                        .build());
        when(mainFbGrpcClient.enterStage("2001", 1, 1))
                .thenReturn(EnterStageResponse.newBuilder()
                        .setBattleId("1675231914_0_2")
                        .build());

        Msgmainfb.PB_CSMainFbReq req = Msgmainfb.PB_CSMainFbReq.newBuilder()
                .setType(0)
                .build();

        AtomicReference<byte[]> payloadRef = new AtomicReference<>();
        try (MockedStatic<Emitters> emitters = mockStatic(Emitters.class)) {
            emitters.when(() -> Emitters.emit(eq(session), eq(11003), any(byte[].class)))
                    .thenAnswer(invocation -> {
                        payloadRef.set(invocation.getArgument(2));
                        return null;
                    });

            mainFbHandler.handle(session, 2005, req.toByteArray()).block();

            verify(mainFbGrpcClient).getCurrentTask("2001");
            verify(mainFbGrpcClient).enterStage("2001", 1, 1);
            assertThat(payloadRef.get()).isNotNull();

            Msgbattle.PB_SCBattleReport report = Msgbattle.PB_SCBattleReport.parseFrom(payloadRef.get());
            assertThat(report.getBattleModeType()).isEqualTo(0);
            assertThat(report.getBattleFileName()).isEqualTo("1675231914_0_2");
        }
    }
}
