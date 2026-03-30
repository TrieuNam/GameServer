package com.SouthMillion.webSocket_server.handler.gem;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.GemFeign;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GemHandlerTest {

    @Mock
    private GemFeign gemFeign;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private GemHandler gemHandler;

    @Test
    void publishTaskProgress_shouldPublishWhenOperationSuccess() {
        ReflectionTestUtils.invokeMethod(
                gemHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", true),
                "condition_82",
                "websocket-gem-inlay"
        );

        verify(taskProgressPublisher).publish(2001L, "condition_82", 1, "websocket-gem-inlay");
    }

    @Test
    void publishTaskProgress_shouldSkipWhenOperationFailed() {
        ReflectionTestUtils.invokeMethod(
                gemHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", false),
                "condition_82",
                "websocket-gem-inlay"
        );

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
