package com.SouthMillion.webSocket_server.handler.escort;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.EscortGrpcClient;
import org.SouthMillion.grpc.escort.EscortActionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscortHandlerTest {

    @Mock
    private EscortGrpcClient escortGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;
    @Mock
    private BagFeign bagFeign;
    @Mock
    private WalletHttpClient walletHttpClient;

    @InjectMocks
    private EscortHandler escortHandler;

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(escortHandler, "publishTaskProgress", 2001L, "condition_86", "websocket-escort-start");

        verify(taskProgressPublisher).publish(2001L, "condition_86", 1, "websocket-escort-start");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(escortHandler, "publishTaskProgress", 2001L, "", "websocket-escort-start");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void handleComplete_refreshesBagAndWalletOnSuccess() {
        PlayerSession session = org.mockito.Mockito.mock(PlayerSession.class);
        when(escortGrpcClient.claimReward(2001L)).thenReturn(EscortActionResponse.newBuilder().setSuccess(true).build());
        when(taskActionConditionMapping.escortCompleteTaskKey()).thenReturn("condition_86");
        when(bagFeign.list("2001")).thenReturn(List.of());
        when(walletHttpClient.info("2001")).thenReturn(null);

        ReflectionTestUtils.invokeMethod(escortHandler, "handleComplete", session, 2001L);

        verify(bagFeign, atLeastOnce()).list("2001");
        verify(walletHttpClient, atLeastOnce()).info("2001");
    }
}
