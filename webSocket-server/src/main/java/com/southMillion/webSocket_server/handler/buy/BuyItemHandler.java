package com.southMillion.webSocket_server.handler.buy;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.handler.item.KnapsackSingleInfoHandler;
import com.southMillion.webSocket_server.service.client.item.ItemFeignClient;
import com.southMillion.webSocket_server.utils.SessionUtils;
import org.SouthMillion.dto.item.Knapsack.ItemConfigDTO;
import org.SouthMillion.dto.item.Knapsack.ItemRetrieveConfigDTO;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class BuyItemHandler implements GameMessageHandler {
    @Autowired
    private ItemFeignClient itemFeign;

    @Autowired
    private ItemNotEnoughHandler itemNotEnoughHandler;

    @Autowired
    private KnapsackSingleInfoHandler knapsackSingleInfoHandler;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgknapsack.PB_CSBuyCmdReq req;
        try {
            req = Msgknapsack.PB_CSBuyCmdReq.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        String userId = SessionUtils.getUserId(session);
        int itemId = req.getBuyParam1();
        int count = req.getNum();

        // ---- Kiểm tra itemId có hợp lệ không qua item-service ----
        boolean valid = false;

        // Kiểm tra item thông thường
        Object config = itemFeign.getItemConfig(itemId);
        if (config != null) {
            valid = true;
        } else {
            // Kiểm tra trong item_retrieve
            ItemRetrieveConfigDTO retrieveConfig = itemFeign.getItemRetrieveConfig();
            if (retrieveConfig != null && retrieveConfig.getRetrieve() != null) {
                valid = retrieveConfig.getRetrieve().stream()
                        .anyMatch(retrieve -> retrieve.getSeq() != null && retrieve.getSeq() == itemId);
            }
        }

        if (!valid) {
            // Gửi thông báo item không hợp lệ
            itemNotEnoughHandler.sendNotEnough(session, itemId);
            return;
        }

        // ---- Đã hợp lệ, tiến hành add item ----
        itemFeign.addItem(userId, itemId, count);

        // Thông báo cho client thông tin item vừa được cập nhật
        knapsackSingleInfoHandler.sendSingleItemInfo(session, userId, itemId);

        Msgknapsack.PB_SCGetItemNotice resp = Msgknapsack.PB_SCGetItemNotice.newBuilder()
                .setGetType(1)
                .addItemList(Msgknapsack.PB_ItemData.newBuilder().setItemId(itemId).setNum(count))
                .build();
        sendResponse(session, 1507, resp.toByteArray());
    }



    @Override
    public int getMsgId() {
        return 1501;
    }
}
