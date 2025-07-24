package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.ItemServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.ItemDto;
import org.SouthMillion.dto.item.KafkaEventDto;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class KnapsackReqHandler implements GameMessageHandler {
    private final ItemServiceFeignClient itemServiceFeign;

    @Autowired
    private WebsocketEventProducer eventProducer; // Bổ sung inject bean này

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msgknapsack.PB_CSKnapsackReq req = Msgknapsack.PB_CSKnapsackReq.parseFrom(payload);
            String userId = (String) session.getAttributes().get("userId");
            // Lấy toàn bộ item inventory user
            List<ItemDto> itemList = itemServiceFeign.list(userId);

            Msgknapsack.PB_SCKnapsackAllInfo.Builder builder = Msgknapsack.PB_SCKnapsackAllInfo.newBuilder();
            for (ItemDto i : itemList) {
                builder.addItemList(Msgknapsack.PB_ItemData.newBuilder()
                        .setItemId(i.getItemId())
                        .setNum(i.getAmount()).build());
            }
            sendResponse(session, 1505, builder.build().toByteArray());

            // Gửi event Kafka
            KafkaEventDto event = KafkaEventDto.builder()
                    .userId(userId)
                    .type("ITEM_ALL")
                    .payload(itemList)
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            eventProducer.sendGameEvent(userId, "KNAPSACK_REQ", event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1500;
    }

}
