package com.southMillion.webSocket_server.handler;

import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class TimeRequestMessageHandler implements GameMessageHandler {
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msgserver.PB_CSTimeReq req = Msgserver.PB_CSTimeReq.parseFrom(payload);

            long now = System.currentTimeMillis() / 1000L;
            // Giả lập giá trị, thực tế bạn có thể lấy từ DB/Redis hoặc config
            long serverStart = 1720800000L;    // ví dụ: fixed timestamp
            int openDays = (int) ((now - serverStart) / (3600 * 24));
            long combineTime = 0L; // nếu chưa có, có thể để 0

            Msgserver.PB_SCTimeAck resp = Msgserver.PB_SCTimeAck.newBuilder()
                    .setServerTime((int) now)
                    .setServerRealStartTime((int) serverStart)
                    .setOpenDays(openDays)
                    .setServerRealCombineTime((int) combineTime)
                    .build();

            sendResponse(session, 9000, resp.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() { return 9050; }

}