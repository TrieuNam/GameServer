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
public class BoxSetReqHandler implements GameMessageHandler {
    @Autowired
    private BoxFeignClient boxFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgbox.PB_CSBoxSetReq req;
        try {
            req = Msgbox.PB_CSBoxSetReq.parseFrom(payload);
        } catch (Exception e) {
            return;
        }
        Long userId = Long.valueOf(getUserIdFromSession(session));
        // Cập nhật thiết lập auto mở hộp cho user
        boxFeign.saveBoxSetting(userId, BoxProtoMapper.toBoxSetDTO(req.getBoxSet()));

        // Trả lại info mới (PB_SCBoxSetingInfo)
        Msgbox.PB_SCBoxSetingInfo resp = BoxProtoMapper.toProtoBoxSetingInfo(boxFeign.getBoxSetting(userId));
        sendResponse(session, 1617, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1611;
    }
}
