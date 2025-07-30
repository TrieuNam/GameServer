package com.southMillion.webSocket_server.handler.buy;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.item.ItemFeignClient;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class ItemNotEnoughHandler implements GameMessageHandler {

    @Override
    public void handle(WebSocketSession session, byte[] payload) { throw new UnsupportedOperationException(); }
    @Override
    public int getMsgId() { return 1504; }

    public void sendNotEnough(WebSocketSession session, int itemId) {
        Msgknapsack.PB_SCItemNotEnoughNotice resp = Msgknapsack.PB_SCItemNotEnoughNotice.newBuilder()
                .setItemId(itemId)
                .build();
        sendResponse(session, 1504, resp.toByteArray());
    }
}