package com.southMillion.webSocket_server.handler.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.LimitCoreInfoDTO;
import org.SouthMillion.mapper.user.RoleProtoMapper;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class LimitCoreHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgother.PB_CSLimitCoreReq req = null;
        try {
            req = Msgother.PB_CSLimitCoreReq.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        Integer uid = getUserIdFromSession(session);
        userFeign.limitCoreOp(uid, req.getType(), req.getP1());
        LimitCoreInfoDTO dto = userFeign.getLimitCoreInfo(uid);
        Msgother.PB_SCLimitCoreInfo resp = RoleProtoMapper.toProto(dto);
        sendResponse(session, 1468, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1467;
    }
}