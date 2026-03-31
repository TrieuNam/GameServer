package com.SouthMillion.webSocket_server.handler.starmap;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.StarMapGrpcClient;
import org.SouthMillion.proto.Msgstarmap.Msgstarmap;
import org.SouthMillion.proto.starmap.ActivateStarResponse;
import org.SouthMillion.proto.starmap.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Sinks;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StarMapHandlerTest {

    @Mock
    private StarMapGrpcClient starMapGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;

    private StarMapHandler starMapHandler;
    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        starMapHandler = new StarMapHandler(starMapGrpcClient, taskProgressPublisher);
        playerSession = PlayerSession.builder()
                .ws(null)
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
        playerSession.setRoleId(2001L);
    }

    @Test
    void handleActivatePublishesCondition35OnSuccess() {
        when(starMapGrpcClient.activateStar(2001L, 7)).thenReturn(ActivateStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                        .setCode(200)
                        .setSuccess(true)
                        .build())
                .build());

        ReflectionTestUtils.invokeMethod(
                starMapHandler,
                "handleActivate",
                playerSession,
                2001L,
                Msgstarmap.PB_CSStarMapReq.newBuilder().setParam1(7).build()
        );

        verify(taskProgressPublisher).publish(2001L, "condition_35", 1, "websocket-starmap-activate");
    }

    @Test
    void handleActivateSkipsPublishOnFailure() {
        when(starMapGrpcClient.activateStar(2001L, 7)).thenReturn(ActivateStarResponse.newBuilder()
                .setStatus(ResponseStatus.newBuilder()
                        .setCode(500)
                        .setSuccess(false)
                        .build())
                .build());

        ReflectionTestUtils.invokeMethod(
                starMapHandler,
                "handleActivate",
                playerSession,
                2001L,
                Msgstarmap.PB_CSStarMapReq.newBuilder().setParam1(7).build()
        );

        verify(taskProgressPublisher, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }
}