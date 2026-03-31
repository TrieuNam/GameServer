package com.SouthMillion.webSocket_server.handler.formation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Sinks;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationHandlerTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping conditionMapping;

    private FormationHandler handler;
    private PlayerSession session;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        handler = new FormationHandler(redis, taskProgressPublisher, conditionMapping, new ObjectMapper());
        session = PlayerSession.builder()
                .ws(null)
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
        session.setRoleId(3001L);
    }

    @Test
    void handleLevelUpPublishesCondition27WithNewLevel() {
        when(valueOps.increment("formation:level:3001")).thenReturn(5L);
        when(conditionMapping.formationLevelUpTaskKey()).thenReturn("condition_27");

        ReflectionTestUtils.invokeMethod(handler, "handleLevelUp", session, 3001L);

        verify(taskProgressPublisher).publish(3001L, "condition_27", 5, "websocket-formation-levelup");
    }

    @Test
    void handleLevelUpSkipsPublishWhenTaskKeyIsNull() {
        when(valueOps.increment("formation:level:3001")).thenReturn(2L);
        when(conditionMapping.formationLevelUpTaskKey()).thenReturn(null);

        ReflectionTestUtils.invokeMethod(handler, "handleLevelUp", session, 3001L);

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void handleSaveSlotsDoesSNotPublishCondition27() {
        when(valueOps.get("formation:level:3001")).thenReturn("3");
        java.util.Map<String, Object> req = new java.util.HashMap<>();
        req.put("formationId", 1);
        req.put("slots", java.util.List.of());

        ReflectionTestUtils.invokeMethod(handler, "handleSaveSlots", session, 3001L, req);

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
