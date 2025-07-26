package com.southMillion.webSocket_server.handler.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.RoleDTO;
import org.SouthMillion.mapper.user.RoleProtoMapper;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class GetOtherRoleInfoHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgrole.PB_CSGetOtherRoleInfo req = null;
        try {
            req = Msgrole.PB_CSGetOtherRoleInfo.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        RoleDTO dto = userFeign.getOtherRoleInfo(req.getUid());
        Msgrole.PB_SCGetOtherRoleRet resp = RoleProtoMapper.toProtoOtherRole(dto);
        sendResponse(session, 1463, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1462;
    }
}