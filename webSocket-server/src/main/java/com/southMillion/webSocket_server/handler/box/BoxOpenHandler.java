package com.southMillion.webSocket_server.handler.box;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.box.BoxFeignClient;
import org.SouthMillion.dto.item.Box.BoxOpenRequestDTO;
import org.SouthMillion.dto.item.Box.BoxRecordDTO;
import org.SouthMillion.mapper.box.BoxProtoMapper;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class BoxOpenHandler implements GameMessageHandler {
    @Autowired
    private BoxFeignClient boxFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msgbox.PB_CSBoxReq req = Msgbox.PB_CSBoxReq.parseFrom(payload);
            BoxOpenRequestDTO dto = BoxProtoMapper.toBoxOpenRequestDTO(req, getUserIdFromSession(session));
            BoxRecordDTO record = boxFeign.openBox(dto);

            // Mapping sang PB_SCBoxEquipInfo (1615)
            Msgbox.PB_SCBoxEquipInfo resp = BoxProtoMapper.toProtoBoxEquipInfo(record);
            sendResponse(session, 1615, resp.toByteArray());
        } catch (Exception e) {
            // log...
        }
    }
    @Override public int getMsgId() { return 1610; }

    private Long getUserIdFromSession(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}
