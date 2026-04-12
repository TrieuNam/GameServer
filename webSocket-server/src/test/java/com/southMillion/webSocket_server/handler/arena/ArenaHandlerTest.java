package com.SouthMillion.webSocket_server.handler.arena;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.ArenaGrpcClient;
import org.SouthMillion.proto.Msgarena.Msgarena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArenaHandlerTest {

    @Mock
    private ArenaGrpcClient arenaGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;
    @Mock
    private BagFeign bagFeign;
    @Mock
    private WalletHttpClient walletHttpClient;

    @InjectMocks
    private ArenaHandler arenaHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = org.mockito.Mockito.mock(PlayerSession.class);
        when(playerSession.getRoleId()).thenReturn(2001L);
    }

    @Test
    void handleChallengePublishesOnVictory() {
        when(arenaGrpcClient.challenge(2001L, 88)).thenReturn(Map.of("victory", true));
        when(taskActionConditionMapping.arenaWinTaskKey()).thenReturn("condition_26");

        ReflectionTestUtils.invokeMethod(arenaHandler, "handleChallenge", playerSession, 88);

        verify(taskProgressPublisher).publish(2001L, "condition_26", 1, "websocket-arena-win");
    }

    @Test
    void handleChallengeSkipsPublishOnDefeat() {
        when(arenaGrpcClient.challenge(2001L, 88)).thenReturn(Map.of("victory", false));

        ReflectionTestUtils.invokeMethod(arenaHandler, "handleChallenge", playerSession, 88);

        verify(taskProgressPublisher, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void handleClaimRewards_refreshesBagAndWallet() {
        when(arenaGrpcClient.claimRewards(2001L, "DAILY")).thenReturn(Map.of("success", true));
        when(arenaGrpcClient.getArenaInfo(2001L)).thenReturn(Map.of());
        when(bagFeign.list("2001")).thenReturn(List.of());
        when(walletHttpClient.info("2001")).thenReturn(null);

        ReflectionTestUtils.invokeMethod(arenaHandler, "handleClaimRewards", playerSession);

        verify(bagFeign, atLeastOnce()).list("2001");
        verify(walletHttpClient, atLeastOnce()).info("2001");
    }

    @Test
    void handleLegacyRefreshRequestRoutesToOpponents() {
        when(arenaGrpcClient.getArenaInfo(2001L)).thenReturn(Map.of());
        when(arenaGrpcClient.getOpponents(2001L, 5)).thenReturn(List.of());

        Msgarena.PB_CSArenaReq req = Msgarena.PB_CSArenaReq.newBuilder()
                .setType(1)
                .build();

        arenaHandler.handle(playerSession, MessageIds.CS_ARENA_REQ, req.toByteArray()).block();

        verify(arenaGrpcClient, atLeastOnce()).getOpponents(2001L, 5);
        verify(arenaGrpcClient, never()).challenge(2001L, 0);
        verify(arenaGrpcClient, never()).getBattleHistory(2001L, 0, 10);
    }

    @Test
    void handleLegacyReportRequestRoutesToBattleHistory() {
        when(arenaGrpcClient.getBattleHistory(2001L, 0, 10)).thenReturn(List.of());

        Msgarena.PB_CSArenaReq req = Msgarena.PB_CSArenaReq.newBuilder()
                .setType(2)
                .build();

        arenaHandler.handle(playerSession, MessageIds.CS_ARENA_REQ, req.toByteArray()).block();

        verify(arenaGrpcClient).getBattleHistory(2001L, 0, 10);
        verify(arenaGrpcClient, never()).challenge(2001L, 0);
    }
}
