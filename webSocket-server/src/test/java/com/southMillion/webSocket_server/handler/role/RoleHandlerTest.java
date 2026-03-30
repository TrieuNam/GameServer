package com.SouthMillion.webSocket_server.handler.role;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.SouthMillion.feign.RoleFeignClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoleHandler
 */
@ExtendWith(MockitoExtension.class)
class RoleHandlerTest {

    @Mock
    private RoleFeignClient roleFeign;

    @InjectMocks
    private RoleHandler roleHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        when(playerSession.getUserId()).thenReturn(1001L);
        when(playerSession.getRoleId()).thenReturn(2001L);
        when(playerSession.getUsername()).thenReturn("testUser");
    }

    @Test
    void testInterests() {
        int[] interests = roleHandler.interests();
        
        assertNotNull(interests);
        assertTrue(interests.length >= 3);
    }

    @Test
    void testHandleWithNullPayload() {
        assertDoesNotThrow(() -> 
            roleHandler.handle(playerSession, MessageIds.CS_ROLE_WX_INFO_SET_REQ, null)
        );
    }

    @Test
    void testHandleSystemSettings() {
        byte[] payload = new byte[32]; // Simple payload
        
        assertDoesNotThrow(() -> 
            roleHandler.handle(playerSession, MessageIds.CS_ROLE_SYSTEM_SET_REQ, payload)
        );
    }

    @Test
    void testHandleSkillOperation() {
        byte[] payload = new byte[16];
        
        // Should log warning but not crash
        assertDoesNotThrow(() -> 
            roleHandler.handle(playerSession, MessageIds.CS_ROLE_SKILL_OPERA_REQ, payload)
        );
    }

    @Test
    void testHandleUnknownMessage() {
        byte[] payload = new byte[16];
        
        assertDoesNotThrow(() -> 
            roleHandler.handle(playerSession, 99999, payload)
        );
    }
}
