package com.SouthMillion.webSocket_server.handler.trial;

import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.TrialGrpcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialHandlerTest {

    @Mock
    private TrialGrpcClient trialGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    private TrialHandler trialHandler;

    @BeforeEach
    void setUp() {
        trialHandler = new TrialHandler(trialGrpcClient, new ObjectMapper(), taskProgressPublisher, taskActionConditionMapping);
    }

    @Test
    void handleCompleteTrialPublishesOnSuccess() {
        when(trialGrpcClient.completeTrial(2001L, 5, 1000L, 3, 120)).thenReturn(Map.of("trialId", 5));
        when(taskActionConditionMapping.trialCompleteTaskKey()).thenReturn("condition_27");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                trialHandler,
                "handleCompleteTrial",
                2001L,
                5,
                new byte[]{0, 0, 0}
        );

        assertEquals(true, result.get("success"));
        verify(taskProgressPublisher).publish(2001L, "condition_27", 1, "websocket-trial-complete");
    }

    @Test
    void handleCompleteTrialSkipsPublishOnFailure() {
        when(trialGrpcClient.completeTrial(2001L, 5, 1000L, 3, 120)).thenReturn(null);

        ReflectionTestUtils.invokeMethod(
                trialHandler,
                "handleCompleteTrial",
                2001L,
                5,
                new byte[]{0, 0, 0}
        );

        verify(taskProgressPublisher, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }
}
