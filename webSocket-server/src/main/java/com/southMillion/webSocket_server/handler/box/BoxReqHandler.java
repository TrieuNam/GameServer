package com.southMillion.webSocket_server.handler.box;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.box.BoxFeignClient;
import org.SouthMillion.mapper.box.BoxProtoMapper;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class BoxReqHandler implements GameMessageHandler {
    @Autowired
    private BoxFeignClient boxFeign; // Giả sử có BoxFeignClient gọi box-service

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgbox.PB_CSBoxReq req;
        try {
            req = Msgbox.PB_CSBoxReq.parseFrom(payload);
        } catch (Exception e) {
            return;
        }
        Long userId = Long.valueOf(getUserIdFromSession(session));
        int reqType = req.getReqType();
        int param = req.getParam();

        // Tùy reqType, bạn gọi service thực hiện mở hộp, mua, bán, vv...
        Msgbox.PB_SCBoxInfo resp = BoxProtoMapper.toProto(boxFeign.handleBoxRequest(userId, reqType, param));
        sendResponse(session, 1616, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1610;
    }
}