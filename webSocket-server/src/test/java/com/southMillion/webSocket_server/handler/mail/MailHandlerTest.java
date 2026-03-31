package com.SouthMillion.webSocket_server.handler.mail;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MailHandler
 */
@ExtendWith(MockitoExtension.class)
class MailHandlerTest {

    @InjectMocks
    private MailHandler mailHandler;

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
        int[] interests = mailHandler.interests();
        
        assertNotNull(interests);
        assertEquals(1, interests.length);
        assertEquals(MessageIds.CS_MAIL_REQ, interests[0]);
    }

    @Test
    void testHandleWithNullPayload() {
        assertDoesNotThrow(() -> 
            mailHandler.handle(playerSession, MessageIds.CS_MAIL_REQ, null)
        );
    }

    @Test
    void testHandleMailListRequest() {
        // opType = 0 (MAIL_OP_LIST)
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(0); // opType
        buffer.putLong(0); // mailId (not used for list)
        
        byte[] payload = buffer.array();
        
        assertDoesNotThrow(() -> 
            mailHandler.handle(playerSession, MessageIds.CS_MAIL_REQ, payload)
        );
    }

    @Test
    void testHandleMailDetailRequest() {
        // opType = 1 (MAIL_OP_DETAIL)
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(1); // opType
        buffer.putLong(12345L); // mailId
        
        byte[] payload = buffer.array();
        
        assertDoesNotThrow(() -> 
            mailHandler.handle(playerSession, MessageIds.CS_MAIL_REQ, payload)
        );
    }

    @Test
    void testHandleFetchAttachment() {
        // opType = 2 (MAIL_OP_FETCH)
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(2); // opType
        buffer.putLong(12345L); // mailId
        
        byte[] payload = buffer.array();
        
        assertDoesNotThrow(() -> 
            mailHandler.handle(playerSession, MessageIds.CS_MAIL_REQ, payload)
        );
    }

    @Test
    void testHandleDeleteMail() {
        // opType = 4 (MAIL_OP_DELETE)
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(4); // opType
        buffer.putLong(12345L); // mailId
        
        byte[] payload = buffer.array();
        
        assertDoesNotThrow(() -> 
            mailHandler.handle(playerSession, MessageIds.CS_MAIL_REQ, payload)
        );
    }

    @Test
    void testHandleInvalidOpType() {
        // opType = 99 (invalid)
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(99); // invalid opType
        buffer.putLong(0);
        
        byte[] payload = buffer.array();
        
        assertDoesNotThrow(() -> 
            mailHandler.handle(playerSession, MessageIds.CS_MAIL_REQ, payload)
        );
    }
}
