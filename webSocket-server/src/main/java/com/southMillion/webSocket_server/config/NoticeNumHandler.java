package com.southMillion.webSocket_server.config;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.globalServiceFeignClient;
import org.SouthMillion.proto.Msgsystem.Msgsystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class NoticeNumHandler implements GameMessageHandler {
    @Autowired
    private globalServiceFeignClient noticeService;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            String userKey = (String) session.getAttributes().get("userKey");
            int noticeNum = noticeService.getUnreadNoticeNum(userKey);

            Msgsystem.PB_SCNoticeNum resp = Msgsystem.PB_SCNoticeNum.newBuilder()
                    .setNoticeNum(noticeNum)
                    .build();

            sendResponse(session, 700, resp.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 700;
    }
}