package com.southMillion.webSocket_server.handler;


import com.southMillion.webSocket_server.service.client.ServerInfoServiceClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.serverInfor.ServerTimeEventDto;
import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class TimeRequestMessageHandler implements GameMessageHandler {

    @Autowired
    private ServerInfoServiceClient serverInfoService;

    @Autowired
    private WebsocketEventProducer eventProducer; // Thêm vào

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msgserver.PB_CSTimeReq req = Msgserver.PB_CSTimeReq.parseFrom(payload);

            long now = System.currentTimeMillis() / 1000L;
            long serverStart = serverInfoService.getServerRealStartTime(); // Lấy từ DB/Redis
            int openDays = (int) ((now - serverStart) / (3600 * 24));
            long combineTime = serverInfoService.getServerCombineTime(); // Nếu có

            Msgserver.PB_SCTimeAck resp = Msgserver.PB_SCTimeAck.newBuilder()
                    .setServerTime((int) now)
                    .setServerRealStartTime((int) serverStart)
                    .setOpenDays(openDays)
                    .setServerRealCombineTime((int) combineTime)
                    .build();

            sendResponse(session, 9000, resp.toByteArray());

            // Gửi event Kafka cho task-service & report-service
            String userId = (session.getAttributes().get("userId") != null)
                    ? String.valueOf(session.getAttributes().get("userId"))
                    : "unknown";

            // Nên wrap payload thành DTO cho dễ maintain
            ServerTimeEventDto eventDto = new ServerTimeEventDto(now, serverStart, openDays, combineTime);

            eventProducer.sendGameEvent(
                    userId,
                    "SERVER_TIME_REQUEST",
                    eventDto
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 9050;
    }

}