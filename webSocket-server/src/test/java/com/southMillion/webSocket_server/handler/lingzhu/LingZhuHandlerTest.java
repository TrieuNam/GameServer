package com.SouthMillion.webSocket_server.handler.lingzhu;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.LingZhuFeign;
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
class LingZhuHandlerTest {

    @Mock
    private LingZhuFeign lingZhuFeign;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private LingZhuHandler lingZhuHandler;

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(lingZhuHandler, "publishTaskProgress", 2001L, "condition_89", "websocket-lingzhu-challenge");

        verify(taskProgressPublisher).publish(2001L, "condition_89", 1, "websocket-lingzhu-challenge");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(lingZhuHandler, "publishTaskProgress", 2001L, "", "websocket-lingzhu-challenge");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
