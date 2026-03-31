package com.SouthMillion.webSocket_server.handler.guild;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.common.ProtoRequestWrapper;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.GuildFeign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuildHandlerTest {

    @Mock
    private GuildFeign guildFeign;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private GuildHandler guildHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = new PlayerSession();
        playerSession.setRoleId(2001L);
    }

    @Test
    void handleCreateGuildPublishesWhenSuccess() {
        ProtoRequestWrapper req = org.mockito.Mockito.mock(ProtoRequestWrapper.class);
        when(req.getStrParam()).thenReturn("TestGuild");
        when(guildFeign.createGuild(anyMap())).thenReturn(Map.of("guildId", 99, "name", "TestGuild"));
        when(taskActionConditionMapping.guildCreateTaskKey()).thenReturn("condition_35");

        ReflectionTestUtils.invokeMethod(guildHandler, "handleCreateGuild", playerSession, req);

        verify(taskProgressPublisher).publish(2001L, "condition_35", 1, "websocket-guild-create");
    }

    @Test
    void handleJoinGuildPublishesOnlyOnSuccess() {
        ProtoRequestWrapper req = org.mockito.Mockito.mock(ProtoRequestWrapper.class);
        when(req.getGuildId()).thenReturn(123);
        when(guildFeign.joinGuild(anyMap())).thenReturn(Map.of("success", true));
        when(taskActionConditionMapping.guildJoinTaskKey()).thenReturn("condition_36");

        ReflectionTestUtils.invokeMethod(guildHandler, "handleJoinGuild", playerSession, req);

        verify(taskProgressPublisher).publish(2001L, "condition_36", 1, "websocket-guild-join");
    }

    @Test
    void handleJoinGuildDoesNotPublishWhenFailed() {
        ProtoRequestWrapper req = org.mockito.Mockito.mock(ProtoRequestWrapper.class);
        when(req.getGuildId()).thenReturn(123);
        when(guildFeign.joinGuild(anyMap())).thenReturn(Map.of("success", false));

        ReflectionTestUtils.invokeMethod(guildHandler, "handleJoinGuild", playerSession, req);

        verify(taskProgressPublisher, never()).publish(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }
}
