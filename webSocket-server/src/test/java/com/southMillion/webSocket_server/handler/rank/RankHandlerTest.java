package com.SouthMillion.webSocket_server.handler.rank;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RankHandler
 */
@ExtendWith(MockitoExtension.class)
class RankHandlerTest {

    @InjectMocks
    private RankHandler rankHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        when(playerSession.getUserId()).thenReturn(1001L);
        when(playerSession.getRoleId()).thenReturn(2001L);
    }

    @Test
    void testInterests() {
        int[] interests = rankHandler.interests();
        
        assertNotNull(interests);
        assertEquals(1, interests.length);
        assertEquals(MessageIds.CS_RANK_REQ, interests[0]);
    }

    @Test
    void testHandleRankRequest() {
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> 
            rankHandler.handle(playerSession, MessageIds.CS_RANK_REQ, payload)
        );
        
        verify(playerSession, atLeastOnce()).send(anyInt(), any(byte[].class));
    }

    @Test
    void testSendPowerRanking() {
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> {
            rankHandler.handle(playerSession, MessageIds.CS_RANK_REQ, payload);
            verify(playerSession).send(eq(MessageIds.SC_RANK_LIST), any(byte[].class));
        });
    }
}
