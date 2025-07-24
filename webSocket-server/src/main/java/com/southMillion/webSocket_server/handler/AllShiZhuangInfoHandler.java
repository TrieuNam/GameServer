package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.ShiZhuangFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ShiZhuang.ShiZhuangDto;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class AllShiZhuangInfoHandler implements GameMessageHandler {
    private final ShiZhuangFeignClient shizhuangFeignClient;

    private static final String TASK_TOPIC = "task-events";
    private static final String REPORT_TOPIC = "report-events";

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        String userId = (String) session.getAttributes().get("userId");
        List<ShiZhuangDto> list = shizhuangFeignClient.list(userId);
        Msgknapsack.PB_SCAllShiZhuangInfo.Builder builder = Msgknapsack.PB_SCAllShiZhuangInfo.newBuilder();
        for (ShiZhuangDto d : list) {
            builder.addShizhuangList(Msgknapsack.PB_ShiZhuangData.newBuilder().setId(d.getId()).setLevel(d.getLevel()));
        }
        sendResponse(session, 1509, builder.build().toByteArray());

    }

    @Override
    public int getMsgId() {
        return 1509;
    }
}