package com.southMillion.webSocket_server.handler.item;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.item.ItemFeignClient;
import com.southMillion.webSocket_server.utils.SessionUtils;
import org.SouthMillion.dto.item.Knapsack.ItemDTO;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class KnapsackHandler implements GameMessageHandler {
    @Autowired
    private ItemFeignClient itemFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        String userId = SessionUtils.getUserId(session);
        List<ItemDTO> items = itemFeign.getItems(userId);

        Msgknapsack.PB_SCKnapsackAllInfo.Builder resp = Msgknapsack.PB_SCKnapsackAllInfo.newBuilder();
        for (ItemDTO dto : items) {
            resp.addItemList(
                    Msgknapsack.PB_ItemData.newBuilder()
                            .setItemId(dto.getItemId())
                            .setNum(dto.getCount())
                            .build()
            );
        }
        sendResponse(session, 1505, resp.build().toByteArray());
    }

    @Override
    public int getMsgId() { return 1500; }
}
