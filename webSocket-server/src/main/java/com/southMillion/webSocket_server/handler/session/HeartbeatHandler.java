package com.southMillion.webSocket_server.handler.session;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.session.SessionFeignClient;
import com.southMillion.webSocket_server.utils.SessionUtils;
import org.SouthMillion.dto.session.HeartbeatRequest;
import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class HeartbeatHandler implements GameMessageHandler {

    @Autowired
    private SessionFeignClient sessionServiceFeignClient;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            // 1. Parse Protobuf
            Msgserver.PB_CSHeartbeatReq req = Msgserver.PB_CSHeartbeatReq.parseFrom(payload);
            System.out.println("Heartbeat received. reserve=" + req.getReserve());

            // 2. Lấy sessionId/roleId (giả sử bạn lưu attribute khi login)
            String sessionId = SessionUtils.getSessionId(session);
            Integer roleId = SessionUtils.getRoleId(session);
            if (sessionId == null || roleId == null) {
                System.out.println("sessionId/roleId not found in session!");
                session.close();
                return;
            }

            // 3. Gọi session-service để update heartbeat
            HeartbeatRequest heartbeatReq = new HeartbeatRequest();
            heartbeatReq.setSessionId(sessionId);
            heartbeatReq.setRoleId(roleId);
            sessionServiceFeignClient.heartbeat(heartbeatReq);

            // 4. Trả về HeartbeatResp (msgId 1003)
            Msgserver.PB_SCHeartbeatResp resp = Msgserver.PB_SCHeartbeatResp.newBuilder()
                    .setReserve(req.getReserve())
                    .build();
            sendResponse(session, 1003, resp.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            // Optionally, bạn có thể đóng kết nối hoặc gửi lỗi cho client
        }
    }

    @Override
    public int getMsgId() {
        return 1053; // Đúng với MsgId của heartbeat request
    }


}