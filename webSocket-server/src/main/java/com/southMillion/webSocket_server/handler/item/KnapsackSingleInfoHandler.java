package com.southMillion.webSocket_server.handler.item;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.item.ItemFeignClient;
import org.SouthMillion.dto.item.Knapsack.ItemDTO;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class KnapsackSingleInfoHandler implements GameMessageHandler {

    @Autowired
    private ItemFeignClient itemFeign;

    // Gọi hàm này từ service nghiệp vụ sau khi có thay đổi item (VD: add, consume, ...)
    public void sendSingleItemInfo(WebSocketSession session, String userId, int itemId) {
        ItemDTO dto = itemFeign.getSingle(userId, itemId);

        if (dto != null) {
            Msgknapsack.PB_SCKnapsackSingleInfo resp = Msgknapsack.PB_SCKnapsackSingleInfo.newBuilder()
                    .setItem(Msgknapsack.PB_ItemData.newBuilder()
                            .setItemId(dto.getItemId())
                            .setNum(dto.getCount())
                            .build())
                    .build();
            sendResponse(session, 1506, resp.toByteArray());
        }
    }

    // Không dùng cho request trực tiếp từ client, nên để mặc định hoặc throw
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        throw new UnsupportedOperationException("Không nhận request trực tiếp từ client cho 1506.");
    }

    @Override
    public int getMsgId() {
        return 1506;
    }
}