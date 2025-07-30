package com.southMillion.webSocket_server.handler.box;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class BoxSellInfoHandler implements GameMessageHandler {
    public void sendBoxSellInfo(WebSocketSession session, Msgbox.PB_SCBoxSellInfo resp) {
        sendResponse(session, 1618, resp.toByteArray());
    }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMsgId() {
        return 1618;
    }
}