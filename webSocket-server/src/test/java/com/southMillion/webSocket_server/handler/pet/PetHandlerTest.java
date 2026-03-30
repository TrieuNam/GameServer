package com.SouthMillion.webSocket_server.handler.pet;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.PetFeign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PetHandler
 */
@ExtendWith(MockitoExtension.class)
class PetHandlerTest {

    @Mock
    private PetFeign petFeign;

    @Mock
    private TaskProgressPublisher taskProgressPublisher;

    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private PetHandler petHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        when(playerSession.getUserId()).thenReturn(1001L);
        when(playerSession.getRoleId()).thenReturn(2001L);
    }

    @Test
    void testInterests() {
        int[] interests = petHandler.interests();
        
        assertNotNull(interests);
        assertEquals(2, interests.length);
        assertEquals(MessageIds.CS_ROLE_PET_REQ, interests[0]);
    }

    @Test
    void testHandleWithNullRoleId() {
        when(playerSession.getRoleId()).thenReturn(null);
        
        assertDoesNotThrow(() -> 
            petHandler.handle(playerSession, MessageIds.CS_ROLE_PET_REQ, new byte[0])
        );
    }

    @Test
    void testHandlePetRequest() {
        byte[] payload = new byte[16];
        
        assertDoesNotThrow(() -> 
            petHandler.handle(playerSession, MessageIds.CS_ROLE_PET_REQ, payload)
        );
        
        verify(playerSession, atLeastOnce()).send(anyInt(), any(byte[].class));
    }

    @Test
    void testHandleOneKeyUpLevelGem() {
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> 
            petHandler.handle(playerSession, MessageIds.CS_PET_ONE_KEY_UP_LEVEL_GEM_REQ, payload)
        );
    }

    @Test
    void publishTaskProgress_shouldPublishOnSuccess() {
        ReflectionTestUtils.invokeMethod(
                petHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", true),
                "condition_51",
                "websocket-pet-activate"
        );

        verify(taskProgressPublisher).publish(2001L, "condition_51", 1, "websocket-pet-activate");
    }

    @Test
    void publishTaskProgress_shouldSkipWhenFailed() {
        ReflectionTestUtils.invokeMethod(
                petHandler,
                "publishTaskProgress",
                2001L,
                Map.of("success", false),
                "condition_51",
                "websocket-pet-activate"
        );

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }
}
