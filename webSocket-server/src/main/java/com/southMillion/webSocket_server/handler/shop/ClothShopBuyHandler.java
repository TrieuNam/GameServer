package com.southMillion.webSocket_server.handler.shop;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.shop.ShopFeignClient;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;

@Component
public class ClothShopBuyHandler implements GameMessageHandler {
    @Autowired
    private ShopFeignClient shopClient;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msgother.PB_CSClothShopBuyReq req = Msgother.PB_CSClothShopBuyReq.parseFrom(payload);
            Long userId = Long.valueOf(getUserIdFromSession(session));

            shopClient.buyCloth(userId, req.getSeq(), req.getNum());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1622;
    }
}