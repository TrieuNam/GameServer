package com.SouthMillion.webSocket_server.handler.territory;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.TerritoryGrpcClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerritoryHandlerTest {

    @Mock
    private TerritoryGrpcClient territoryGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private TerritoryHandler territoryHandler;

    @Test
    void publishTaskProgress_fetchReward_shouldPublish() {
        when(taskActionConditionMapping.territoryFetchRewardTaskKey()).thenReturn("condition_87");

        ReflectionTestUtils.invokeMethod(territoryHandler, "publishTaskProgress", 2001L, 1);

        verify(taskProgressPublisher).publish(2001L, "condition_87", 1, "websocket-territory-fetch-reward");
    }

    @Test
    void publishTaskProgress_unknownAction_shouldSkip() {
        ReflectionTestUtils.invokeMethod(territoryHandler, "publishTaskProgress", 2001L, 99);

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
