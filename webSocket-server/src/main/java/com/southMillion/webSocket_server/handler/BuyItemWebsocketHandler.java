package com.southMillion.webSocket_server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.southMillion.webSocket_server.service.client.ItemServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.BuyItemRequest;
import org.SouthMillion.dto.item.ItemDto;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.Map;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class BuyItemWebsocketHandler implements GameMessageHandler {
    private final ItemServiceFeignClient itemServiceFeign;
    private final WebsocketEventProducer eventProducer;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            // Parse protobuf request
            Msgknapsack.PB_CSBuyCmdReq req = Msgknapsack.PB_CSBuyCmdReq.parseFrom(payload);
            String userId = (String) session.getAttributes().get("userId"); // lấy userId từ session/login

            int itemId = req.getBuyParam1();
            int amount = req.getNum();

            // Gọi Feign để thực hiện mua
            try {
                itemServiceFeign.buy(userId, itemId, amount);

                // Nếu thành công, trả về thông tin item mới cho client (có thể gọi lại get)
                ItemDto item = itemServiceFeign.get(userId, itemId);

                Msgknapsack.PB_SCKnapsackSingleInfo resp = Msgknapsack.PB_SCKnapsackSingleInfo.newBuilder()
                        .setItem(Msgknapsack.PB_ItemData.newBuilder()
                                .setItemId(item.getItemId())
                                .setNum(item.getAmount())
                                .build())
                        .build();

                sendResponse(session, 1506, resp.toByteArray());

                // Gửi event Kafka
                eventProducer.sendGameEvent(userId, "ITEM_BOUGHT", item);

            } catch (Exception e) {
                // Nếu mua thất bại (ví dụ: Not enough item), trả về thông báo lỗi protobuf
                // Có thể customize lỗi phù hợp, hoặc gửi PB_SCItemNotEnoughNotice

                Msgknapsack.PB_SCItemNotEnoughNotice err = Msgknapsack.PB_SCItemNotEnoughNotice.newBuilder()
                        .setItemId(itemId)
                        .build();

                sendResponse(session, 1504, err.toByteArray());


                eventProducer.sendGameEvent(userId, "ITEM_NOT_ENOUGH", itemId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Có thể trả về error message chung nếu parse lỗi
        }
    }

    @Override
    public int getMsgId() { return 1501; }

}
