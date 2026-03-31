package com.SouthMillion.webSocket_server.handler.shop;

import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.ShopFeign;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.shop.ResultDTO;
import org.SouthMillion.dto.shop.ShopDTOs;
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
 * Unit tests for ShopHandler
 */
@ExtendWith(MockitoExtension.class)
class ShopHandlerTest {

    @Mock
    private ShopFeign shopFeign;

    @Mock
    private BagFeign bagFeign;

    @InjectMocks
    private ShopHandler shopHandler;

    private PlayerSession playerSession;

    @BeforeEach
    void setUp() {
        playerSession = mock(PlayerSession.class);
        when(playerSession.getUserId()).thenReturn(1001L);
        when(playerSession.getRoleId()).thenReturn(2001L);
        when(playerSession.getSessionId()).thenReturn("session_123");
    }

    @Test
    void testInterests() {
        int[] interests = shopHandler.interests();
        
        assertNotNull(interests);
        assertEquals(3, interests.length);
        assertEquals(MessageIds.CS_SHOP_BUY_REQ, interests[0]);
        assertEquals(MessageIds.CS_CLOTH_SHOP_BUY_REQ, interests[1]);
        assertEquals(MessageIds.CS_MYSTERY_SHOP_REQ, interests[2]);
    }

    @Test
    void testHandleWithNullRoleId() {
        when(playerSession.getRoleId()).thenReturn(null);
        
        assertDoesNotThrow(() -> 
            shopHandler.handle(playerSession, MessageIds.CS_SHOP_BUY_REQ, new byte[0])
        );
        
        verifyNoInteractions(shopFeign);
    }

    @Test
    void testHandleShopBuySuccess() {
        // Mock successful purchase
        ShopDTOs.BuyResp buyResp = new ShopDTOs.BuyResp(true, "Success");
        ResultDTO<ShopDTOs.BuyResp> result = ResultDTO.ok(buyResp);
        when(shopFeign.buy(any(ShopDTOs.BuyReq.class))).thenReturn(result);
        
        // Mock bag items
        List<BagDTOs.ItemView> mockItems = new ArrayList<>();
        mockItems.add(BagDTOs.ItemView.builder().itemId(100).itemName("PurchasedItem").num(1).build());
        when(bagFeign.list(anyString())).thenReturn(mockItems);
        
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> 
            shopHandler.handle(playerSession, MessageIds.CS_SHOP_BUY_REQ, payload)
        );
        
        verify(shopFeign, times(1)).buy(any(ShopDTOs.BuyReq.class));
        verify(bagFeign, times(1)).list("role_2001");
        verify(playerSession, times(1)).send(eq(MessageIds.SC_GET_ITEM_NOTICE), any(byte[].class));
    }

    @Test
    void testHandleShopBuyFailure() {
        // Mock failed purchase
        ShopDTOs.BuyResp buyResp = new ShopDTOs.BuyResp(false, "Insufficient currency");
        ResultDTO<ShopDTOs.BuyResp> result = ResultDTO.ok(buyResp);
        when(shopFeign.buy(any(ShopDTOs.BuyReq.class))).thenReturn(result);
        
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> 
            shopHandler.handle(playerSession, MessageIds.CS_SHOP_BUY_REQ, payload)
        );
        
        verify(shopFeign, times(1)).buy(any(ShopDTOs.BuyReq.class));
        verifyNoInteractions(bagFeign); // Should not query bag on failure
    }

    @Test
    void testHandleClothShopBuy() {
        ShopDTOs.BuyResp buyResp = new ShopDTOs.BuyResp(true, "Success");
        ResultDTO<ShopDTOs.BuyResp> result = ResultDTO.ok(buyResp);
        when(shopFeign.buy(any(ShopDTOs.BuyReq.class))).thenReturn(result);
        
        List<BagDTOs.ItemView> mockItems = new ArrayList<>();
        when(bagFeign.list(anyString())).thenReturn(mockItems);
        
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> 
            shopHandler.handle(playerSession, MessageIds.CS_CLOTH_SHOP_BUY_REQ, payload)
        );
        
        verify(shopFeign, times(1)).buy(any(ShopDTOs.BuyReq.class));
    }

    @Test
    void testHandleMysteryShop() {
        List<ShopDTOs.ShopItem> items = new ArrayList<>();
        items.add(new ShopDTOs.ShopItem("1", "RareItem", 100L, 10L, 0L, 0L, 0, 0));
        ShopDTOs.ShopListResp shopList = new ShopDTOs.ShopListResp(items);
        ResultDTO<ShopDTOs.ShopListResp> result = ResultDTO.ok(shopList);
        
        when(shopFeign.listMystery(anyInt(), any())).thenReturn(result);
        
        byte[] payload = new byte[4];
        
        assertDoesNotThrow(() -> 
            shopHandler.handle(playerSession, MessageIds.CS_MYSTERY_SHOP_REQ, payload)
        );
        
        verify(shopFeign, times(1)).listMystery(anyInt(), any());
        verify(playerSession, times(1)).send(eq(MessageIds.SC_MYSTERY_SHOP_INFO), any(byte[].class));
    }

    @Test
    void testHandleWithException() {
        when(shopFeign.buy(any(ShopDTOs.BuyReq.class))).thenThrow(new RuntimeException("Network error"));
        
        byte[] payload = new byte[8];
        
        assertDoesNotThrow(() -> 
            shopHandler.handle(playerSession, MessageIds.CS_SHOP_BUY_REQ, payload)
        );
        
        // Should send error response
        verify(playerSession, times(1)).send(eq(MessageIds.SC_GET_ITEM_NOTICE), any(byte[].class));
    }
}
