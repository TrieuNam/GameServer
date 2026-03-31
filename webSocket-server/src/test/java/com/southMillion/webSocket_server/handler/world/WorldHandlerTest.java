package com.SouthMillion.webSocket_server.handler.world;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.PlayerSessionRegistry;
import com.SouthMillion.webSocket_server.service.client.WorldFeign;
import com.SouthMillion.webSocket_server.service.grpc.GameWorldGrpcClient;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.gameworld.NearbyPlayersResponse;
import org.SouthMillion.grpc.gameworld.PlayerInfo;
import org.SouthMillion.grpc.gameworld.UpdatePositionResponse;
import org.SouthMillion.proto.Msgworld.Msgworld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldHandlerTest {

    @Mock
    private GameWorldGrpcClient gameWorldGrpcClient;

    @Mock
    private WorldFeign worldFeign;

    @Mock
    private PlayerSessionRegistry sessionRegistry;

    @Mock
    private PlayerSession session;

    private WorldHandler worldHandler;

    @BeforeEach
    void setUp() {
        worldHandler = new WorldHandler(gameWorldGrpcClient, worldFeign, sessionRegistry);
        ReflectionTestUtils.setField(worldHandler, "feignScheduler", Schedulers.immediate());
    }

    @Test
    void moveShouldQueryNearbyPlayersInsteadOfZoneInfo() {
        when(session.getRoleId()).thenReturn(1001L);
        when(session.getCurrentSceneId()).thenReturn(1);
        when(sessionRegistry.getByRoleId(anyLong())).thenReturn(Optional.empty());

        UpdatePositionResponse updateResp = UpdatePositionResponse.newBuilder()
                .setSuccess(true)
                .setStatus(ResponseStatus.newBuilder().setSuccess(true).build())
                .build();
        when(gameWorldGrpcClient.updatePosition(anyLong(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(updateResp);

        NearbyPlayersResponse nearbyResp = NearbyPlayersResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder().setSuccess(true).build())
                .addPlayers(PlayerInfo.newBuilder().setRoleId(1001L).build())
                .addPlayers(PlayerInfo.newBuilder().setRoleId(2002L).build())
                .build();
        when(gameWorldGrpcClient.getNearbyPlayers(anyLong(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyInt()))
                .thenReturn(nearbyResp);

        Msgworld.PB_CSMoveReq req = Msgworld.PB_CSMoveReq.newBuilder()
                .setStartPos(Msgworld.PB_Position.newBuilder().setX(10).setY(0).setZ(10).build())
                .setEndPos(Msgworld.PB_Position.newBuilder().setX(12).setY(0).setZ(13).build())
                .setDirection(Msgworld.PB_Position.newBuilder().setX(1).setY(0).setZ(0).build())
                .build();

        assertDoesNotThrow(() -> worldHandler.handle(session, 2010, req.toByteArray()).block());

        verify(gameWorldGrpcClient).getNearbyPlayers(anyLong(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyInt());
        verify(sessionRegistry).getByRoleId(2002L);
    }
}

