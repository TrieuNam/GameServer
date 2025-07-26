package com.southMillion.webSocket_server.handler.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.GMCommandResultDTO;
import org.SouthMillion.proto.Msggm.Msggm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class GMCommandHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msggm.PB_CSGMCommand req = null;
        try {
            req = Msggm.PB_CSGMCommand.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        Integer uid = getUserIdFromSession(session);
        userFeign.sendGMCommand(uid, req.getType().toByteArray(), req.getCommand().toByteArray());
        GMCommandResultDTO result = userFeign.getGMCommandResult(uid);
        Msggm.PB_SCGMCommand resp = Msggm.PB_SCGMCommand.newBuilder()
                .setType(com.google.protobuf.ByteString.copyFrom(result.getType()))
                .setResult(com.google.protobuf.ByteString.copyFrom(result.getResult()))
                .build();
        sendResponse(session, 2000, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 2001; // GMCommand message ID
    }
}