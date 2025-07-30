package com.southMillion.webSocket_server.handler.user;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.user.UserGameFeignClient;
import com.southMillion.webSocket_server.utils.SessionUtils;
import org.SouthMillion.dto.user.RoleInfoDTO;
import org.SouthMillion.mapper.user.UserProtoMapper;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleInfoHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userService;

    @Override
    public int getMsgId() { return 1400; }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Integer roleId = SessionUtils.getRoleId(session); // Lấy từ session khi login
        if (roleId == null) {
            sendResponse(session, 1400, Msgrole.PB_SCRoleInfoAck.newBuilder().build().toByteArray());
            return;
        }
        RoleInfoDTO dto = userService.getRoleInfo(roleId);
        Msgrole.PB_SCRoleInfoAck.Builder resp = Msgrole.PB_SCRoleInfoAck.newBuilder();
        if (dto != null) {
            resp.setCurExp(dto.getExp() != null ? dto.getExp() : 0);
            resp.setCreateTime(dto.getCreateTime() != null ? dto.getCreateTime() : 0);
            resp.setRoleinfo(UserProtoMapper.toProto(dto));
        }
        sendResponse(session, 1400, resp.build().toByteArray());
    }
}