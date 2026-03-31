package com.SouthMillion.webSocket_server.handler.shizhuang;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.EquipFumoFeign;
import com.SouthMillion.webSocket_server.service.client.ShiZhuangFeign;
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
class ShiZhuangHandlerTest {

    @Mock
    private ShiZhuangFeign shiZhuangFeign;
    @Mock
    private EquipFumoFeign equipFumoFeign;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private ShiZhuangHandler shiZhuangHandler;

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(shiZhuangHandler, "publishTaskProgress", 2001L, "condition_90", "websocket-shizhuang-equip");

        verify(taskProgressPublisher).publish(2001L, "condition_90", 1, "websocket-shizhuang-equip");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(shiZhuangHandler, "publishTaskProgress", 2001L, "", "websocket-shizhuang-equip");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
