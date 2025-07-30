package com.southMillion.webSocket_server.handler.shop;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.shop.ShopFeignClient;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class ShopInfoHandler implements GameMessageHandler {
    @Autowired
    private ShopFeignClient shopClient;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Long userId = Long.valueOf(getUserIdFromSession(session));
        Msgother.PB_SCShopInfo resp = shopClient.getShopInfo(userId);
        sendResponse(session, 1621, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1621;
    }
}