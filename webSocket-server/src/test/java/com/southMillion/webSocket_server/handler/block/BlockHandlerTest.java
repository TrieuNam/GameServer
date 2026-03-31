package com.SouthMillion.webSocket_server.handler.block;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BlockFeign;
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
class BlockHandlerTest {

    @Mock
    private BlockFeign blockFeign;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private BlockHandler blockHandler;

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(blockHandler, "publishTaskProgress", 2001L, "condition_91", "websocket-block-compose");

        verify(taskProgressPublisher).publish(2001L, "condition_91", 1, "websocket-block-compose");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(blockHandler, "publishTaskProgress", 2001L, "", "websocket-block-compose");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
