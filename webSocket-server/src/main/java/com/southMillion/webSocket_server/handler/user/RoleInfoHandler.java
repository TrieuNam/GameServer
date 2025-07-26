package com.southMillion.webSocket_server.handler.user;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.RoleDTO;
import org.SouthMillion.mapper.user.RoleProtoMapper;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleInfoHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Integer uid = getUserIdFromSession(session);
        RoleDTO dto = userFeign.getRoleInfo(uid);
        Msgrole.PB_SCRoleInfoAck resp = RoleProtoMapper.toProto(dto); // mapping DTO→Proto
        sendResponse(session, 1400, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1400;
    }
}
