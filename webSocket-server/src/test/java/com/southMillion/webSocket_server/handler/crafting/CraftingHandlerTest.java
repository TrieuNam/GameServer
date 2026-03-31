package com.SouthMillion.webSocket_server.handler.crafting;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.CraftingGrpcClient;
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

@ExtendWith(MockitoExtension.class)
class CraftingHandlerTest {

    @Mock
    private CraftingGrpcClient craftingGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private CraftingHandler craftingHandler;

    @Test
    void publishTaskProgress_shouldPublishWhenKeyPresent() {
        ReflectionTestUtils.invokeMethod(craftingHandler, "publishTaskProgress", 2001L, "condition_81", "websocket-crafting-start");

        verify(taskProgressPublisher).publish(2001L, "condition_81", 1, "websocket-crafting-start");
    }

    @Test
    void publishTaskProgress_shouldSkipWhenKeyBlank() {
        ReflectionTestUtils.invokeMethod(craftingHandler, "publishTaskProgress", 2001L, "", "websocket-crafting-start");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
