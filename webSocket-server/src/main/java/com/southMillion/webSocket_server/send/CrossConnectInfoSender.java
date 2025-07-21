package com.southMillion.webSocket_server.send;

import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class CrossConnectInfoSender {
    public void send(WebSocketSession session, int isConnectedCross, int isCross) {
        Msgserver.PB_SCCrossConnectInfo info = Msgserver.PB_SCCrossConnectInfo.newBuilder()
                .setIsConnectedCross(isConnectedCross)
                .setIsCross(isCross)
                .build();
        sendResponse(session, 9003, info.toByteArray());
    }
}
