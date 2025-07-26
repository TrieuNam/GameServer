package com.southMillion.webSocket_server.handler.user;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.RoleAttrListDTO;
import org.SouthMillion.mapper.user.RoleProtoMapper;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleAttrListHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Integer uid = getUserIdFromSession(session);
        RoleAttrListDTO dto = userFeign.getRoleAttrList(uid);
        Msgrole.PB_SCRoleAttrList resp = RoleProtoMapper.toProto(dto);
        sendResponse(session, 1401, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1401;
    }
}