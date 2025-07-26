package com.southMillion.webSocket_server.handler.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.UserGameFeignClient;
import org.SouthMillion.dto.user.RoleSystemSetDTO;
import org.SouthMillion.dto.user.SystemSetDTO;
import org.SouthMillion.mapper.user.RoleProtoMapper;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.stream.Collectors;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.getUserIdFromSession;
import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleSystemSetHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userFeign;
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Integer uid = getUserIdFromSession(session);
        Msgrole.PB_CSRoleSystemSetReq req = null;
        try {
            req = Msgrole.PB_CSRoleSystemSetReq.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        List<SystemSetDTO> list = req.getSystemSetListList().stream()
                .map(RoleProtoMapper::fromProto)
                .collect(Collectors.toList());
        userFeign.setSystemSettings(uid, list);
        RoleSystemSetDTO dto = userFeign.getSystemSettings(uid);
        Msgrole.PB_SCRoleSystemSetInfo resp = RoleProtoMapper.toProto(dto);
        sendResponse(session, 1461, resp.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1460;
    }
}
