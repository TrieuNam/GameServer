package com.SouthMillion.webSocket_server.handler.rune;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.RuneGrpcClient;
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
class RuneHandlerTest {

    @Mock
    private RuneGrpcClient runeGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private RuneHandler runeHandler;

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(runeHandler, "publishTaskProgress", 2001L, "condition_85", "websocket-rune-level-up");

        verify(taskProgressPublisher).publish(2001L, "condition_85", 1, "websocket-rune-level-up");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(runeHandler, "publishTaskProgress", 2001L, "", "websocket-rune-level-up");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
