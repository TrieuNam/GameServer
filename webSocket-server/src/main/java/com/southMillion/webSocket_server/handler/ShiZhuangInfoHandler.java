package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.ShiZhuangFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ShiZhuang.ShiZhuangDto;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class ShiZhuangInfoHandler implements GameMessageHandler {
    private final ShiZhuangFeignClient shizhuangFeignClient;

    private final WebsocketEventProducer eventProducer; // Bổ sung inject

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(payload, 4, 4);
            int id = buf.getInt();

            String userId = (String) session.getAttributes().get("userId");

            ShiZhuangDto d = shizhuangFeignClient.get(userId, id);

            Msgknapsack.PB_SCShiZhuangInfo resp = Msgknapsack.PB_SCShiZhuangInfo.newBuilder()
                    .setShizhuang(Msgknapsack.PB_ShiZhuangData.newBuilder()
                            .setId(d.getId())
                            .setLevel(d.getLevel()))
                    .build();
            sendResponse(session, 1510, resp.toByteArray());

            // Gửi event Kafka cho task-service & report-service
            eventProducer.sendGameEvent(
                    userId,
                    "SHIZHUANG_INFO",
                    d
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1510;
    }

}