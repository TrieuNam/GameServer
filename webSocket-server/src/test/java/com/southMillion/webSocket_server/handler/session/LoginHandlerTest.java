package com.SouthMillion.webSocket_server.handler.session;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginHandler
 */
@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    @InjectMocks
    private LoginHandler loginHandler;

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
        int[] interests = loginHandler.interests();
        
        assertNotNull(interests);
        assertTrue(interests.length > 0);
        
        // Check if CS_LOGIN_TO_ACCOUNT is in interests
        boolean hasLoginMessage = false;
        for (int msgId : interests) {
            if (msgId == MessageIds.CS_LOGIN_TO_ACCOUNT) {
                hasLoginMessage = true;
                break;
            }
        }
        assertTrue(hasLoginMessage, "LoginHandler should handle CS_LOGIN_TO_ACCOUNT");
    }

    @Test
    void testHandleWithNullPayload() {
        // Should not throw exception
        assertDoesNotThrow(() -> 
            loginHandler.handle(playerSession, MessageIds.CS_LOGIN_TO_ACCOUNT, null)
        );
    }

    @Test
    void testHandleWithValidPayload() {
        // Create a simple payload
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.putLong(System.currentTimeMillis()); // loginTime
        buffer.put("testToken".getBytes()); // loginStr
        
        byte[] payload = buffer.array();
        
        // Should not throw exception
        assertDoesNotThrow(() -> 
            loginHandler.handle(playerSession, MessageIds.CS_LOGIN_TO_ACCOUNT, payload)
        );
    }

    @Test
    void testHandleHeartbeat() {
        byte[] emptyPayload = new byte[0];
        
        assertDoesNotThrow(() -> 
            loginHandler.handle(playerSession, MessageIds.CS_HEARTBEAT_REQ, emptyPayload)
        );
    }

    @Test
    void testHandleTimeSync() {
        byte[] emptyPayload = new byte[0];
        
        assertDoesNotThrow(() -> 
            loginHandler.handle(playerSession, MessageIds.CS_TIME_REQ, emptyPayload)
        );
    }
}
