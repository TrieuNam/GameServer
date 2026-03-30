package com.SouthMillion.webSocket_server.handler.shenqi;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.ArtifactFeign;
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
class ShenQiHandlerTest {

    @Mock
    private ArtifactFeign artifactFeign;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private ShenQiHandler shenQiHandler;

    @Test
    void publishTaskProgress_shouldPublishWhenSuccess() {
        ReflectionTestUtils.invokeMethod(
                shenQiHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", true),
                "condition_83",
                "websocket-shenqi-upgrade"
        );

        verify(taskProgressPublisher).publish(2001L, "condition_83", 1, "websocket-shenqi-upgrade");
    }

    @Test
    void publishTaskProgress_shouldSkipWhenFailed() {
        ReflectionTestUtils.invokeMethod(
                shenQiHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", false),
                "condition_83",
                "websocket-shenqi-upgrade"
        );

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
