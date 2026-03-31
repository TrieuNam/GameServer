package com.SouthMillion.webSocket_server.handler.arena;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.ArenaGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArenaHandlerTest {

    @Mock
    private ArenaGrpcClient arenaGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private ArenaHandler arenaHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = new PlayerSession();
        playerSession.setRoleId(2001L);
    }

    @Test
    void handleChallengePublishesOnVictory() {
        when(arenaGrpcClient.challenge(2001L, 88)).thenReturn(Map.of("victory", true));
        when(taskActionConditionMapping.arenaWinTaskKey()).thenReturn("condition_26");

        ReflectionTestUtils.invokeMethod(arenaHandler, "handleChallenge", playerSession, 88);

        verify(taskProgressPublisher).publish(2001L, "condition_26", 1, "websocket-arena-win");
    }

    @Test
    void handleChallengeSkipsPublishOnDefeat() {
        when(arenaGrpcClient.challenge(2001L, 88)).thenReturn(Map.of("victory", false));

        ReflectionTestUtils.invokeMethod(arenaHandler, "handleChallenge", playerSession, 88);

        verify(taskProgressPublisher, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }
}
