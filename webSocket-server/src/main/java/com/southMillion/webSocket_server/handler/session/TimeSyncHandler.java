package com.southMillion.webSocket_server.handler.session;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.session.SessionFeignClient;
import org.SouthMillion.dto.session.TimeAckDTO;
import org.SouthMillion.mapper.session.SessionMapper;
import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class TimeSyncHandler implements GameMessageHandler {

    @Autowired
    private SessionFeignClient sessionClient;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        TimeAckDTO dto = sessionClient.getTime();
        Msgserver.PB_SCTimeAck ack = SessionMapper.toProto(dto);
        sendResponse(session, 9000, ack.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 9050;
    }
}