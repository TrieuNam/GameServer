package com.southMillion.webSocket_server.handler.box;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class BoxEquipInfoHandler implements GameMessageHandler {
    public void sendBoxEquipInfo(WebSocketSession session, Msgbox.PB_SCBoxEquipInfo resp) {
        sendResponse(session, 1615, resp.toByteArray());
    }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        throw new UnsupportedOperationException("Không nhận request trực tiếp từ client.");
    }
    @Override
    public int getMsgId() { return 1615; }
}