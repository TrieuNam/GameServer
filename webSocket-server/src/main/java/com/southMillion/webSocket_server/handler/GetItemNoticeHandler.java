package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.ItemNoticeDto;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class GetItemNoticeHandler implements GameMessageHandler {

    private final WebsocketEventProducer eventProducer; // Thêm vào


    // Nếu cần có thể dùng Feign để lấy thông tin mới nhất, hoặc dùng event push từ service khác
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(payload);
            buf.position(4); // bỏ qua msgId
            int itemId = buf.getInt();
            long num = buf.getLong();

            Msgknapsack.PB_SCGetItemNotice resp = Msgknapsack.PB_SCGetItemNotice.newBuilder()
                    .setGetType(1)
                    .addItemList(Msgknapsack.PB_ItemData.newBuilder().setItemId(itemId).setNum(num))
                    .build();
            sendResponse(session, 1507, resp.toByteArray());

            // Gửi event Kafka cho task-service & report-service
            eventProducer.sendGameEvent(
                    (String) session.getAttributes().get("userId"),
                    "ITEM_NOTICE",
                    new ItemNoticeDto(itemId, num)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override public int getMsgId() { return 1507; }
}
