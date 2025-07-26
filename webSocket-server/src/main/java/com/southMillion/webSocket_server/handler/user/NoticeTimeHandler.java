package com.southMillion.webSocket_server.handler.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class NoticeTimeHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgother.PB_CSNoticeTimeReq req = null;
        try {
            req = Msgother.PB_CSNoticeTimeReq.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        Integer uid = getUserIdFromSession(session);
        if (req.getType() == 0) {
            long noticeTime = userFeign.getNoticeTime(uid);
            Msgother.PB_SCNoticeTimeRet resp = Msgother.PB_SCNoticeTimeRet.newBuilder()
                    .setNoticeTime(noticeTime).build();
            sendResponse(session, 1465, resp.toByteArray());
        } else {
            userFeign.setNoticeTime(uid, req.getParam());
            Msgother.PB_SCNoticeTimeRet resp = Msgother.PB_SCNoticeTimeRet.newBuilder()
                    .setNoticeTime(req.getParam()).build();
            sendResponse(session, 1465, resp.toByteArray());
        }
    }

    @Override
    public int getMsgId() {
        return 1464;
    }
}