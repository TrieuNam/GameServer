package com.SouthMillion.webSocket_server.handler.mount;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.MountFeign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MountHandler
 */
@ExtendWith(MockitoExtension.class)
class MountHandlerTest {

    @Mock
    private MountFeign mountFeign;

    @Mock
    private TaskProgressPublisher taskProgressPublisher;

    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private MountHandler mountHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        when(playerSession.getUserId()).thenReturn(1001L);
        when(playerSession.getRoleId()).thenReturn(2001L);
    }

    @Test
    void testInterests() {
        int[] interests = mountHandler.interests();
        
        assertNotNull(interests);
        assertEquals(1, interests.length);
        assertEquals(MessageIds.CS_MOUNT_REQ, interests[0]);
    }

    @Test
    void testHandleMountRequest() {
        byte[] payload = new byte[12];
        
        assertDoesNotThrow(() -> 
            mountHandler.handle(playerSession, MessageIds.CS_MOUNT_REQ, payload)
        );
        
        verify(playerSession, atLeastOnce()).send(anyInt(), any(byte[].class));
    }

    @Test
    void publishTaskProgress_levelUp_shouldPublish() {
        when(taskActionConditionMapping.mountLevelUpTaskKey()).thenReturn("condition_61");

        ReflectionTestUtils.invokeMethod(mountHandler, "publishTaskProgress", 2001L, 1);

        verify(taskProgressPublisher).publish(2001L, "condition_61", 1, "websocket-mount-level-up");
    }

    @Test
    void publishTaskProgress_unknownOp_shouldSkip() {
        ReflectionTestUtils.invokeMethod(mountHandler, "publishTaskProgress", 2001L, 999);

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
