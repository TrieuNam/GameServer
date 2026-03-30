package com.SouthMillion.webSocket_server.handler.bag;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import org.SouthMillion.dto.bag.BagDTOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BagHandler
 */
@ExtendWith(MockitoExtension.class)
class BagHandlerTest {

    @Mock
    private BagFeign bagFeign;

    @Mock
    private WalletHttpClient walletHttpClient;

    @InjectMocks
    private BagHandler bagHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        when(playerSession.getUserId()).thenReturn(1001L);
        when(playerSession.getRoleId()).thenReturn(2001L);
        when(playerSession.getUsername()).thenReturn("testUser");
        when(playerSession.getSessionId()).thenReturn("session_123");
    }

    @Test
    void testInterests() {
        int[] interests = bagHandler.interests();
        
        assertNotNull(interests);
        assertEquals(2, interests.length);
        assertEquals(MessageIds.CS_KNAPSACK_REQ, interests[0]);
        assertEquals(MessageIds.CS_BUY_CMD_REQ, interests[1]);
    }

    @Test
    void testHandleWithNullRoleId() {
        when(playerSession.getRoleId()).thenReturn(null);
        
        // Should log warning and return early
        assertDoesNotThrow(() -> 
            bagHandler.handle(playerSession, MessageIds.CS_KNAPSACK_REQ, new byte[0])
        );
        
        // Verify no service calls were made
        verifyNoInteractions(bagFeign);
    }

    @Test
    void testHandleKnapsackRequest() {
        // Mock bag service response
        List<BagDTOs.ItemView> mockItems = new ArrayList<>();
        mockItems.add(BagDTOs.ItemView.builder().itemId(1).itemName("Item1").num(10).build());
        mockItems.add(BagDTOs.ItemView.builder().itemId(2).itemName("Item2").num(5).build());
        
        when(bagFeign.list(anyString())).thenReturn(mockItems);
        
        // Create simple payload
        byte[] payload = new byte[]{0, 0, 0, 0}; // reqType = 0
        
        assertDoesNotThrow(() -> 
            bagHandler.handle(playerSession, MessageIds.CS_KNAPSACK_REQ, payload)
        );
        
        // Verify bag service was called
        verify(bagFeign, times(1)).list("role_2001");
        
        // Verify response was sent
        verify(playerSession, times(1)).send(eq(MessageIds.SC_KNAPSACK_ALL_INFO), any(byte[].class));
    }

    @Test
    void testHandleBuyCmdRequest() {
        // Mock granted items
        List<BagDTOs.ItemView> mockGrantedItems = new ArrayList<>();
        mockGrantedItems.add(BagDTOs.ItemView.builder().itemId(100).itemName("SpecialCard").num(1).build());
        
        when(bagFeign.grant(any(BagDTOs.GrantReq.class))).thenReturn(mockGrantedItems);
        
        byte[] payload = new byte[32]; // Simple payload
        
        assertDoesNotThrow(() -> 
            bagHandler.handle(playerSession, MessageIds.CS_BUY_CMD_REQ, payload)
        );
        
        // Verify grant was called
        verify(bagFeign, times(1)).grant(any(BagDTOs.GrantReq.class));
        
        // Verify notification was sent
        verify(playerSession, times(1)).send(eq(MessageIds.SC_GET_ITEM_NOTICE), any(byte[].class));
    }

    @Test
    void testHandleUnknownMessage() {
        byte[] payload = new byte[0];
        
        assertDoesNotThrow(() -> 
            bagHandler.handle(playerSession, 99999, payload)
        );
        
        // Verify no service calls
        verifyNoInteractions(bagFeign);
    }

    @Test
    void testHandleWithServiceException() {
        when(bagFeign.list(anyString())).thenThrow(new RuntimeException("Service unavailable"));
        
        byte[] payload = new byte[4];
        
        // Should handle exception gracefully
        assertDoesNotThrow(() -> 
            bagHandler.handle(playerSession, MessageIds.CS_KNAPSACK_REQ, payload)
        );
    }
}
